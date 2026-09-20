package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.customer.Customer;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = DbFields.Tables.SALES_ORDER,
        indexes = {@Index(name = DbFields.Indexes.ORDER_OUTLET_CLOSED,
                columnList = DbFields.OUTLET_ID + "," + DbFields.CLOSED_AT),
                @Index(name = DbFields.Indexes.ORDER_INVOICE,
                        columnList = DbFields.OUTLET_ID + "," + DbFields.INVOICE_NUMBER, unique = true)})
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("order")
public class SalesOrder extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.DRAFT;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private String tableReference;
    @Column(nullable = false)
    private long orderNumber;
    private String invoiceNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType = DiscountType.NONE;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal dueAmount = BigDecimal.ZERO;
    private Instant closedAt;
    private Instant preparedAt;
    private Instant cancelledAt;
    @Column(nullable = false)
    private long dashboardRevision;
}
