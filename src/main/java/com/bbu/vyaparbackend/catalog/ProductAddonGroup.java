package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "addon_group_id"}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("productaddon")
public class ProductAddonGroup extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AddonGroup addonGroup;
}
