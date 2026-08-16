package com.bbu.vyaparbackend.payment;

import com.bbu.vyaparbackend.order.OrderItem;
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
@PrefixedId("ritem")
public class RefundItem extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Refund refund;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private OrderItem orderItem;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
}
