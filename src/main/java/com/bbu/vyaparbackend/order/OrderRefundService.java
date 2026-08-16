package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.inventory.InventoryLedgerService;
import com.bbu.vyaparbackend.payment.Refund;
import com.bbu.vyaparbackend.payment.RefundService;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderRefundService {
    private final OrderQueryService orders;
    private final RefundService refunds;
    private final InventoryLedgerService inventory;

    public OrderRefundService(OrderQueryService orders, RefundService refunds, InventoryLedgerService inventory) {
        this.orders = orders;
        this.refunds = refunds;
        this.inventory = inventory;
    }

    @Transactional
    public Refund refund(Outlet outlet, String orderId, User actor, OrderCommands.Refund command) {
        SalesOrder order = orders.get(outlet, orderId);
        if (order.getStatus() != OrderStatus.CLOSED) throw ApiException.conflict("Only closed orders can be refunded");
        BigDecimal already = refunds.totalForOrder(order.getId());
        if (command.amount().add(already).compareTo(order.getPaidAmount()) > 0) {
            throw ApiException.invalid("Refund exceeds collected payments");
        }
        Refund refund = refunds.create(order, actor, command.amount(), command.restoreStock(), command.reason());
        for (OrderCommands.RefundLine line : command.items()) {
            OrderItem orderItem = orders.requireItem(order, line.orderItemId());
            BigDecimal returned = refunds.totalForOrderItem(orderItem.getId());
            if (line.quantity().add(returned).compareTo(orderItem.getQuantity()) > 0) {
                throw ApiException.invalid("Refund quantity exceeds sold quantity for " + orderItem.getProductName());
            }
            refunds.addItem(refund, orderItem, line.quantity());
            if (command.restoreStock()) {
                inventory.restoreRecipe(outlet, orderItem.getProduct(), line.quantity(), refund.getId());
            }
        }
        BigDecimal totalRefund = already.add(refund.getAmount());
        order.setPaymentStatus(totalRefund.compareTo(order.getPaidAmount()) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
        return refund;
    }
}
