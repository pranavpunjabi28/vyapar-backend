package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.catalog.Ingredient;
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
@PrefixedId("pitem")
public class PurchaseItem extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Purchase purchase;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Ingredient ingredient;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitCost;
}
