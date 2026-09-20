package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.payment.Payment;
import com.bbu.vyaparbackend.payment.PaymentMethod;
import com.bbu.vyaparbackend.shared.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.bbu.vyaparbackend.shared.Pageables.requireAllowedSort;

@Service
public class OrderQueryService {
    private final SalesOrderRepository orders;
    private final OrderItemRepository items;
    private final OrderItemAddonRepository itemAddons;

    OrderQueryService(SalesOrderRepository orders, OrderItemRepository items,
                      OrderItemAddonRepository itemAddons) {
        this.orders = orders;
        this.items = items;
        this.itemAddons = itemAddons;
    }

    @Transactional(readOnly = true)
    public Page<SalesOrder> list(Outlet outlet, Pageable pageable) {
        return list(outlet, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SalesOrder> list(Outlet outlet, LocalDate date, Pageable pageable) {
        pageable = requireAllowedSort(pageable,
                Set.of("id", "createdAt", "closedAt", "invoiceNumber", "status", "total"),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (date != null) {
            DateRange range = dateRange(outlet, date);
            return orders.findAllByOutletIdAndArchivedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    outlet.getId(), range.from(), range.to(), pageable);
        }
        return orders.findAllByOutletIdAndArchivedFalse(outlet.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public OrderApi.DailySummaryView dailySummary(Outlet outlet, LocalDate date) {
        DateRange range = dateRange(outlet, date);
        DailyOrderSummaryProjection summary = orders.dailySummary(outlet.getId(), range.from(), range.to());
        return new OrderApi.DailySummaryView(date, summary.getTotalOrders(), summary.getHeldOrders(),
                summary.getPreparingOrders(), summary.getCompletedOrders(), summary.getCancelledOrders(),
                summary.getUnpaidOrders(), summary.getCompletedSales());
    }

    @Transactional(readOnly = true)
    public List<SalesOrder> recentSubmitted(Outlet outlet, LocalDate date) {
        DateRange range = dateRange(outlet, date);
        return orders.findTop5ByOutletIdAndArchivedFalseAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                outlet.getId(), OrderStatus.DRAFT, range.from(), range.to());
    }

    @Transactional(readOnly = true)
    public Page<SalesOrder> report(Outlet outlet, OrderStatus status, Instant from, Instant to, String customerId,
                                   PaymentMethod method, Pageable pageable) {
        Specification<SalesOrder> specification = (root, query, builder) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(builder.equal(root.get("outlet").get("id"), outlet.getId()));
            predicates.add(builder.isFalse(root.get("archived")));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(builder.lessThan(root.get("createdAt"), to));
            if (customerId != null) predicates.add(builder.equal(root.get("customer").get("id"), customerId));
            if (method != null) {
                jakarta.persistence.criteria.Subquery<Integer> paymentExists = query.subquery(Integer.class);
                jakarta.persistence.criteria.Root<Payment> payment = paymentExists.from(Payment.class);
                paymentExists.select(builder.literal(1)).where(
                        builder.equal(payment.get("order"), root), builder.isFalse(payment.get("archived")),
                        builder.equal(payment.get("method"), method));
                predicates.add(builder.exists(paymentExists));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return orders.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public SalesOrder get(Outlet outlet, String orderId) {
        return orders.findById(orderId)
                .filter(order -> !order.isArchived() && order.getOutlet().getId().equals(outlet.getId()))
                .orElseThrow(() -> ApiException.notFound("Order"));
    }

    @Transactional(readOnly = true)
    public List<OrderItem> items(String orderId) {
        return items.findAllByOrderIdAndArchivedFalseOrderByCreatedAtAscIdAsc(orderId);
    }

    @Transactional(readOnly = true)
    public Map<String, List<OrderItem>> itemsByOrderIds(Collection<String> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        return items.findAllByOrderIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(orderIds).stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));
    }

    @Transactional(readOnly = true)
    public List<OrderItemAddon> addons(String orderItemId) {
        return itemAddons.findAllByOrderItemIdAndArchivedFalseOrderByCreatedAtAscIdAsc(orderItemId);
    }

    @Transactional(readOnly = true)
    public Map<String, List<OrderItemAddon>> addonsByOrderItemIds(Collection<String> orderItemIds) {
        if (orderItemIds.isEmpty()) return Map.of();
        return itemAddons.findAllByOrderItemIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(orderItemIds).stream()
                .collect(Collectors.groupingBy(addon -> addon.getOrderItem().getId()));
    }

    @Transactional(readOnly = true)
    public OrderItem requireItem(SalesOrder order, String itemId) {
        return items.findById(itemId).filter(item -> item.getOrder().getId().equals(order.getId()))
                .orElseThrow(() -> ApiException.notFound("Order item"));
    }

    @Transactional(readOnly = true)
    public Page<ItemAggregate> itemReport(Outlet outlet, Instant from, Instant to, Pageable pageable) {
        return items.itemReport(outlet.getId(), from, to, pageable)
                .map(row -> new ItemAggregate((String) row[0], (String) row[1], (BigDecimal) row[2],
                        (BigDecimal) row[3]));
    }

    @Transactional(readOnly = true)
    public Summary summary(Outlet outlet, Instant from, Instant to) {
        OrderSummaryProjection projection = orders.summary(outlet.getId(), from, to);
        return new Summary(projection.getOrders(), projection.getSales());
    }

    public record ItemAggregate(String productId, String productName, BigDecimal quantity, BigDecimal revenue) {
    }

    public record Summary(long orders, BigDecimal sales) {
    }

    private DateRange dateRange(Outlet outlet, LocalDate date) {
        ZoneId zone = ZoneId.of(outlet.getTimezone());
        return new DateRange(date.atStartOfDay(zone).toInstant(), date.plusDays(1).atStartOfDay(zone).toInstant());
    }

    private record DateRange(Instant from, Instant to) {
    }
}
