package com.bbu.vyaparbackend.payment;

import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.order.OrderCalculator;
import com.bbu.vyaparbackend.order.OrderItem;
import com.bbu.vyaparbackend.order.SalesOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RefundService {
    private final RefundRepository refunds;
    private final RefundItemRepository refundItems;
    private final OrderCalculator calculator;

    RefundService(RefundRepository refunds, RefundItemRepository refundItems, OrderCalculator calculator) {
        this.refunds = refunds;
        this.refundItems = refundItems;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public BigDecimal totalForOrder(String orderId) {
        return refunds.totalForOrder(orderId);
    }

    @Transactional(readOnly = true)
    public BigDecimal totalForOrderItem(String orderItemId) {
        return refundItems.totalForOrderItem(orderItemId);
    }

    @Transactional
    public Refund create(SalesOrder order, User actor, BigDecimal amount, boolean restoreStock, String reason) {
        Refund refund = new Refund();
        refund.setOrder(order);
        refund.setCreatedBy(actor);
        refund.setAmount(calculator.money(amount));
        refund.setRestoreStock(restoreStock);
        refund.setReason(reason);
        return refunds.save(refund);
    }

    @Transactional
    public void addItem(Refund refund, OrderItem orderItem, BigDecimal quantity) {
        RefundItem item = new RefundItem();
        item.setRefund(refund);
        item.setOrderItem(orderItem);
        item.setQuantity(quantity);
        refundItems.save(item);
    }

    @Transactional(readOnly = true)
    public List<RefundItem> items(String refundId) {
        return refundItems.findAllByRefundIdAndArchivedFalseOrderByCreatedAtAscIdAsc(refundId);
    }
}
