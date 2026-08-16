package com.bbu.vyaparbackend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

interface RefundItemRepository extends JpaRepository<RefundItem, String> {
    List<RefundItem> findAllByRefundIdAndArchivedFalseOrderByCreatedAtAscIdAsc(String refundId);

    @Query("select coalesce(sum(i.quantity),0) from RefundItem i where i.orderItem.id=:orderItemId and i.archived=false")
    BigDecimal totalForOrderItem(@Param("orderItemId") String orderItemId);
}
