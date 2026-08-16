package com.bbu.vyaparbackend.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
interface RefundRepository extends JpaRepository<Refund, String> {
    @Query("select coalesce(sum(r.amount),0) from Refund r where r.order.id=:orderId and r.archived=false")
    BigDecimal totalForOrder(@Param("orderId") String orderId);
}
