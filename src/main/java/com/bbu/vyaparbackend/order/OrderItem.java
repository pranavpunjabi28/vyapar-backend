package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.catalog.Product;
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
@PrefixedId("oitem")
public class OrderItem extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private SalesOrder order;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;
    @Column(nullable = false)
    private String productName;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;
    @Column(length = 500)
    private String note;
}
