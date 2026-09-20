package com.bbu.vyaparbackend.report;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface DashboardEventRepository extends JpaRepository<DashboardEvent, String> {
    @Query("select e from DashboardEvent e where e.processedAt is null and e.nextAttemptAt<=:now order by e.createdAt,e.id")
    List<DashboardEvent> pending(@Param("now") Instant now, Pageable pageable);

    @Query(value = "select * from dashboard_event where processed_at is null and next_attempt_at<=:now order by created_at,id limit :limit for update skip locked", nativeQuery = true)
    List<DashboardEvent> claim(@Param("now") long now, @Param("limit") int limit);

    @Query(value = "select pg_try_advisory_xact_lock(761928451)", nativeQuery = true)
    boolean tryWorkerLock();
}

interface OutletOrderSummaryContributionRepository extends JpaRepository<OutletOrderSummaryContribution, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OutletOrderSummaryContribution> findByOrderId(String orderId);
}

interface OutletDailySummaryRepository extends JpaRepository<OutletDailySummary, String> {
    Optional<OutletDailySummary> findByOutletIdAndBusinessDayStartAt(String outletId, Instant businessDayStartAt);

    List<OutletDailySummary> findAllByOutletIdAndBusinessDayStartAtGreaterThanEqualAndBusinessDayStartAtLessThanOrderByBusinessDayStartAt(
            String outletId, Instant from, Instant to);

    List<OutletDailySummary> findAllByOutletId(String outletId);

    @Modifying
    @Query(value = """
            insert into outlet_daily_summary
                (id,version,created_at,updated_at,archived,outlet_id,business_day_start_at,received_orders,
                 completed_orders,cancelled_orders,gross_sales,discount_amount,refund_amount,net_sales,unpaid_amount)
            values (:id,0,:now,:now,false,:outletId,:day,:received,:completed,:cancelled,:gross,:discount,:refund,:net,:unpaid)
            on conflict (outlet_id,business_day_start_at) do update set
                version=outlet_daily_summary.version+1,updated_at=excluded.updated_at,
                received_orders=outlet_daily_summary.received_orders+excluded.received_orders,
                completed_orders=outlet_daily_summary.completed_orders+excluded.completed_orders,
                cancelled_orders=outlet_daily_summary.cancelled_orders+excluded.cancelled_orders,
                gross_sales=outlet_daily_summary.gross_sales+excluded.gross_sales,
                discount_amount=outlet_daily_summary.discount_amount+excluded.discount_amount,
                refund_amount=outlet_daily_summary.refund_amount+excluded.refund_amount,
                net_sales=outlet_daily_summary.net_sales+excluded.net_sales,
                unpaid_amount=outlet_daily_summary.unpaid_amount+excluded.unpaid_amount
            """, nativeQuery = true)
    void applyDelta(@Param("id") String id, @Param("now") long now, @Param("outletId") String outletId,
                    @Param("day") long day, @Param("received") long received, @Param("completed") long completed,
                    @Param("cancelled") long cancelled, @Param("gross") BigDecimal gross,
                    @Param("discount") BigDecimal discount, @Param("refund") BigDecimal refund,
                    @Param("net") BigDecimal net, @Param("unpaid") BigDecimal unpaid);
}
