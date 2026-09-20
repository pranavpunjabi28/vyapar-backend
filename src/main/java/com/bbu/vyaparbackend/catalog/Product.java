package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
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
@PrefixedId("product")
public class Product extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @ManyToOne(fetch = FetchType.LAZY)
    private Category category;
    @Column(nullable = false)
    private String name;
    private String sku;
    @Column(length = 1000)
    private String description;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(nullable = false)
    private boolean active = true;
}
