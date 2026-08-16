package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {DbFields.OUTLET_ID, DbFields.NAME}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("ingredient")
public class Ingredient extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal lowStockThreshold = BigDecimal.ZERO;
}
