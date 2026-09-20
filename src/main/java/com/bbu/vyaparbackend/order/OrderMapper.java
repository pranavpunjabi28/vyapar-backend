package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.payment.Payment;
import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.payment.PaymentService;
import com.bbu.vyaparbackend.payment.Refund;
import com.bbu.vyaparbackend.payment.RefundService;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public final class OrderMapper {
    private OrderMapper() {
    }

    public static Page<OrderApi.OrderView> toViews(Page<SalesOrder> page, Outlet outlet, OrderQueryService orders,
                                                   PaymentService payments) {
        List<String> ids = page.getContent().stream().map(SalesOrder::getId).toList();
        Map<String, List<OrderItem>> itemMap = orders.itemsByOrderIds(ids);
        Map<String, List<OrderItemAddon>> addonMap = orders.addonsByOrderItemIds(itemMap.values().stream()
                .flatMap(List::stream).map(OrderItem::getId).toList());
        Map<String, List<Payment>> paymentMap = payments.listByOrderIds(ids);
        return page.map(order -> toView(order, outlet, itemMap.getOrDefault(order.getId(), List.of()),
                addonMap, paymentMap.getOrDefault(order.getId(), List.of())));
    }

    static OrderApi.OrderView toView(SalesOrder order, Outlet outlet, OrderQueryService orders, PaymentService payments) {
        List<OrderItem> items = orders.items(order.getId());
        return toView(order, outlet, items, orders.addonsByOrderItemIds(items.stream().map(OrderItem::getId).toList()),
                payments.list(order.getId()));
    }

    static OrderApi.OrderView toView(SalesOrder order, Outlet outlet, List<OrderItem> items,
                                     Map<String, List<OrderItemAddon>> addons, List<Payment> payments) {
        return new OrderApi.OrderView(order.getId(), order.getOrderNumber(), order.getStatus(), order.getPaymentStatus(),
                order.getTableReference(), order.getInvoiceNumber(),
                order.getCustomer() == null ? null : order.getCustomer().getId(),
                order.getCustomer() == null ? null : order.getCustomer().getName(),
                order.getCustomer() == null ? null : order.getCustomer().getPhone(), order.getDiscountType(),
                order.getDiscountValue(), order.getSubtotal(), order.getDiscountAmount(), order.getTotal(),
                order.getPaidAmount(), order.getDueAmount(), OrderCancellationRules.canCancel(order, outlet, java.time.Instant.now()),
                OrderCancellationRules.deadline(order, outlet), order.getCreatedAt(), order.getPreparedAt(),
                order.getClosedAt(),
                items.stream().map(item -> new OrderApi.OrderItemView(item.getId(), item.getProduct().getId(),
                                item.getProductName(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal(),
                                item.getNote(), addons.getOrDefault(item.getId(), List.of()).stream()
                                .map(addon -> new OrderApi.OrderItemAddonView(addon.getId(),
                                        addon.getAddonGroup().getId(), addon.getAddonOption().getId(),
                                        addon.getGroupName(), addon.getOptionName(), addon.getUnitPrice()))
                                .toList()))
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
