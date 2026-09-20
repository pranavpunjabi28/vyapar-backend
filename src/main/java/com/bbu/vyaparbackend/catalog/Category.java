package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("category")
public class Category extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private int displayOrder;
}
