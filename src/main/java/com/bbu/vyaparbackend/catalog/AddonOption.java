package com.bbu.vyaparbackend.catalog;

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
@PrefixedId("addonoption")
public class AddonOption extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AddonGroup addonGroup;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private boolean active = true;
}
