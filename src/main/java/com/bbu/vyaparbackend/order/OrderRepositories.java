package com.bbu.vyaparbackend.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface SalesOrderRepository extends JpaRepository<SalesOrder, String>, JpaSpecificationExecutor<SalesOrder> {
    Page<SalesOrder> findAllByOutletIdAndArchivedFalse(String outletId, Pageable pageable);

    List<SalesOrder> findAllByOutletIdAndStatusAndClosedAtGreaterThanEqualAndClosedAtLessThan(String outletId, OrderStatus status, Instant from, Instant to);

    @Query("select count(o) as orders,coalesce(sum(o.total),0) as sales from SalesOrder o where o.outlet.id=:outletId and o.status='CLOSED' and o.closedAt>=:from and o.closedAt<:to and o.archived=false")
    OrderSummaryProjection summary(@Param("outletId") String outletId, @Param("from") Instant from,
                                   @Param("to") Instant to);
}

interface OrderSummaryProjection {
    long getOrders();

    java.math.BigDecimal getSales();
}

interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    List<OrderItem> findAllByOrderIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String orderId);

    List<OrderItem> findAllByOrderIdInAndArchivedFalseOrderByCreatedAtAscIdAsc(Collection<String> orderIds);

    void deleteAllByOrderId(String orderId);

    @Query(value = "select i.product.id,i.productName,sum(i.quantity),sum(i.lineTotal) from OrderItem i where i.order.outlet.id=:outletId and i.order.status='CLOSED' and i.order.closedAt>=:from and i.order.closedAt<:to and i.archived=false group by i.product.id,i.productName order by sum(i.quantity) desc,i.product.id asc",
            countQuery = "select count(distinct i.product.id) from OrderItem i where i.order.outlet.id=:outletId and i.order.status='CLOSED' and i.order.closedAt>=:from and i.order.closedAt<:to and i.archived=false")
    org.springframework.data.domain.Page<Object[]> itemReport(@Param("outletId") String outletId,
                                                              @Param("from") Instant from,
                                                              @Param("to") Instant to,
                                                              Pageable pageable);
}
