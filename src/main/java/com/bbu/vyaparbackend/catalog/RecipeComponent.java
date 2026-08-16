package com.bbu.vyaparbackend.catalog;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {DbFields.PRODUCT_ID, DbFields.INGREDIENT_ID}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("recipe")
public class RecipeComponent extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Ingredient ingredient;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;
}
