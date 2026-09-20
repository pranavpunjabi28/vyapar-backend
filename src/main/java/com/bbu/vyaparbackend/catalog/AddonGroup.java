package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "addon_group", uniqueConstraints = @UniqueConstraint(columnNames = {"outlet_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("addongroup")
public class AddonGroup extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private int displayOrder;
    @Column(nullable = false)
    private int maximumSelections = 1;
}
