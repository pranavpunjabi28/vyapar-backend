package com.bbu.vyaparbackend.report;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "outlet_daily_summary", uniqueConstraints = @UniqueConstraint(name = "uk_outlet_daily_summary_day",
        columnNames = {"outlet_id", "business_day_start_at"}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("daysummary")
class OutletDailySummary extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @Column(nullable = false)
    private Instant businessDayStartAt;
    @Column(nullable = false)
    private long receivedOrders;
    @Column(nullable = false)
    private long completedOrders;
    @Column(nullable = false)
    private long cancelledOrders;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal grossSales = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal refundAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal netSales = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unpaidAmount = BigDecimal.ZERO;
}
