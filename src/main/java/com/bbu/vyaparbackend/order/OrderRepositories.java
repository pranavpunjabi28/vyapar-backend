package com.bbu.vyaparbackend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface SalesOrderRepository extends JpaRepository<SalesOrder, String>, JpaSpecificationExecutor<SalesOrder> {
    @Override
    @EntityGraph(attributePaths = "customer")
    Optional<SalesOrder> findById(String id);

    @EntityGraph(attributePaths = "customer")
    Page<SalesOrder> findAllByOutletIdAndArchivedFalse(String outletId, Pageable pageable);

    @EntityGraph(attributePaths = "customer")
    Page<SalesOrder> findAllByOutletIdAndArchivedFalseAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            String outletId, Instant from, Instant to, Pageable pageable);

    List<SalesOrder> findTop5ByOutletIdAndArchivedFalseAndStatusNotAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            String outletId, OrderStatus status, Instant from, Instant to);

    List<SalesOrder> findAllByOutletIdAndStatusAndClosedAtGreaterThanEqualAndClosedAtLessThan(String outletId, OrderStatus status, Instant from, Instant to);

    @Query("select count(o) as orders,coalesce(sum(o.total),0) as sales from SalesOrder o where o.outlet.id=:outletId and o.status='CLOSED' and o.closedAt>=:from and o.closedAt<:to and o.archived=false")
    OrderSummaryProjection summary(@Param("outletId") String outletId, @Param("from") Instant from,
                                   @Param("to") Instant to);

    @Query("""
            select count(o) as totalOrders,
                   coalesce(sum(case when o.status='HELD' then 1 else 0 end),0) as heldOrders,
                   coalesce(sum(case when o.status='PREPARING' then 1 else 0 end),0) as preparingOrders,
                   coalesce(sum(case when o.status='CLOSED' then 1 else 0 end),0) as completedOrders,
                   coalesce(sum(case when o.status='CANCELLED' then 1 else 0 end),0) as cancelledOrders,
                   coalesce(sum(case when o.status<>'CANCELLED' and o.dueAmount>0 then 1 else 0 end),0) as unpaidOrders,
                   coalesce(sum(case when o.status='CLOSED' then o.total else 0 end),0) as completedSales
            from SalesOrder o
            where o.outlet.id=:outletId and o.createdAt>=:from and o.createdAt<:to and o.archived=false
            """)
    DailyOrderSummaryProjection dailySummary(@Param("outletId") String outletId, @Param("from") Instant from,
                                             @Param("to") Instant to);
}

interface DailyOrderSummaryProjection {
    long getTotalOrders();

    long getHeldOrders();

    long getPreparingOrders();

    long getCompletedOrders();

    long getCancelledOrders();

    long getUnpaidOrders();

    java.math.BigDecimal getCompletedSales();
}

interface OrderSummaryProjection {
    long getOrders();

    java.math.BigDecimal getSales();
}

interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findAllByOrderIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String orderId);

    List<OrderItem> findAllByOrderIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(Collection<String> orderIds);

    void deleteAllByOrderId(String orderId);

    @Query(value = "select i.product.id,i.productName,sum(i.quantity),sum(i.lineTotal) from OrderItem i where i.order.outlet.id=:outletId and i.order.status='CLOSED' and i.order.createdAt>=:from and i.order.createdAt<:to and i.archived=false group by i.product.id,i.productName order by sum(i.quantity) desc,i.product.id asc",
            countQuery = "select count(distinct i.product.id) from OrderItem i where i.order.outlet.id=:outletId and i.order.status='CLOSED' and i.order.createdAt>=:from and i.order.createdAt<:to and i.archived=false")
    org.springframework.data.domain.Page<Object[]> itemReport(@Param("outletId") String outletId,
                                                              @Param("from") Instant from,
                                                              @Param("to") Instant to,
                                                              Pageable pageable);
}

interface OrderItemAddonRepository extends JpaRepository<OrderItemAddon, String> {
    @EntityGraph(attributePaths = {"orderItem", "addonGroup", "addonOption"})
    List<OrderItemAddon> findAllByOrderItemIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String orderItemId);

    @EntityGraph(attributePaths = {"orderItem", "addonGroup", "addonOption"})
    List<OrderItemAddon> findAllByOrderItemIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(Collection<String> orderItemIds);

    void deleteAllByOrderItemOrderId(String orderId);
}
