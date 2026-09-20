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

@Entity
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("productimage")
public class ProductImage extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;
    @Column(nullable = false)
    private String objectKey;
    @Column(nullable = false)
    private int displayOrder;
}
