package com.bbu.vyaparbackend.payment;

import com.bbu.vyaparbackend.order.SalesOrder;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("payment")
public class Payment extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SalesOrder order;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    private String customMethod;
    private String reference;
}
