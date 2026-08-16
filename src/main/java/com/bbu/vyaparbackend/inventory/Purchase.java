package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("purchase")
public class Purchase extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @ManyToOne(fetch = FetchType.LAZY)
    private Supplier supplier;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseStatus status = PurchaseStatus.DRAFT;
    @Column(nullable = false)
    private Instant purchasedAt = Instant.now();
    private String referenceNumber;
    @Column(length = 1000)
    private String note;
}
