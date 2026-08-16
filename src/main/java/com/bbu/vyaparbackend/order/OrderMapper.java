package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.payment.Payment;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.bbu.vyaparbackend.payment.Refund;
import com.bbu.vyaparbackend.payment.RefundService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public final class OrderMapper {
    private OrderMapper() {
    }

    public static Page<OrderApi.OrderView> toViews(Page<SalesOrder> page, OrderQueryService orders,
                                                   PaymentService payments) {
        List<String> ids = page.getContent().stream().map(SalesOrder::getId).toList();
        Map<String, List<OrderItem>> itemMap = orders.itemsByOrderIds(ids);
        Map<String, List<Payment>> paymentMap = payments.listByOrderIds(ids);
        return page.map(order -> toView(order, itemMap.getOrDefault(order.getId(), List.of()),
                paymentMap.getOrDefault(order.getId(), List.of())));
    }

    static OrderApi.OrderView toView(SalesOrder order, OrderQueryService orders, PaymentService payments) {
        return toView(order, orders.items(order.getId()), payments.list(order.getId()));
    }

    static OrderApi.OrderView toView(SalesOrder order, List<OrderItem> items, List<Payment> payments) {
        return new OrderApi.OrderView(order.getId(), order.getStatus(), order.getPaymentStatus(),
                order.getTableReference(), order.getInvoiceNumber(),
                order.getCustomer() == null ? null : order.getCustomer().getId(), order.getDiscountType(),
                order.getDiscountValue(), order.getSubtotal(), order.getDiscountAmount(), order.getTotal(),
                order.getPaidAmount(), order.getDueAmount(), order.getCreatedAt(), order.getClosedAt(),
                items.stream().map(item -> new OrderApi.OrderItemView(item.getId(), item.getProduct().getId(),
                                item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal(), item.getNote()))
                        .toList(),
                payments.stream().map(payment -> new OrderApi.PaymentView(payment.getId(), payment.getMethod(),
                                payment.getAmount(), payment.getCustomMethod(), payment.getReference(), payment.getCreatedAt()))
                        .toList());
    }

    static OrderApi.RefundView toView(Refund refund, RefundService refunds) {
        return new OrderApi.RefundView(refund.getId(), refund.getAmount(), refund.isRestoreStock(), refund.getReason(),
                refund.getCreatedAt(), refunds.items(refund.getId()).stream()
                .map(item -> new OrderApi.RefundItemView(item.getOrderItem().getId(), item.getQuantity())).toList());
    }
}
