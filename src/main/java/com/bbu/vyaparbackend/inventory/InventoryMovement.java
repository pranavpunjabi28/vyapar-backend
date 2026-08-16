package com.bbu.vyaparbackend.inventory;

import com.bbu.vyaparbackend.business.Outlet;
import com.bbu.vyaparbackend.catalog.Ingredient;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(indexes = {@Index(name = DbFields.Indexes.MOVEMENT_STOCK,
        columnList = DbFields.OUTLET_ID + "," + DbFields.INGREDIENT_ID + "," + DbFields.CREATED_AT)})
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("movement")
public class InventoryMovement extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Ingredient ingredient;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
    @Column(nullable = false, length = 40)
    private String referenceType;
    @Column(nullable = false)
    private String referenceId;
    @Column(length = 500)
    private String note;
}
