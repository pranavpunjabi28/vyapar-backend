package com.bbu.vyaparbackend.payment;

import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.order.SalesOrder;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("refund")
public class Refund extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SalesOrder order;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User createdBy;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    @Column(nullable = false)
    private boolean restoreStock;
    @Column(nullable = false, length = 500)
    private String reason;
}
