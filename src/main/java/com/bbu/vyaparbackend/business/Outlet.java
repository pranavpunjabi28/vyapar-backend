package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {DbFields.BUSINESS_ID, DbFields.NAME}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("outlet")
public class Outlet extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Business business;
    @Column(nullable = false)
    private String name;
    private String phone;
    @Column(length = 1000)
    private String address;
    @Column(nullable = false, length = 3)
    private String currency = "INR";
    @Column(nullable = false)
    private String timezone = "Asia/Kolkata";
    private String upiId;
    @Column(length = 500)
    private String receiptFooter;
    @Column(nullable = false)
    private long nextInvoiceNumber = 1;
    @Column(nullable = false)
    private long nextOrderNumber = 1;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderCancellationPolicy preparingOrderCancellationPolicy = OrderCancellationPolicy.ALWAYS;
    @Column(nullable = false)
    private int preparingOrderCancellationMinutes = 10;
}
