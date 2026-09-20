package com.bbu.vyaparbackend.order;

import com.bbu.vyaparbackend.catalog.AddonGroup;
import com.bbu.vyaparbackend.catalog.AddonOption;
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

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_addon", uniqueConstraints = @UniqueConstraint(columnNames = {"order_item_id", "addon_option_id"}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("orderaddon")
public class OrderItemAddon extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private OrderItem orderItem;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AddonGroup addonGroup;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AddonOption addonOption;
    @Column(nullable = false)
    private String groupName;
    @Column(nullable = false)
    private String optionName;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal unitPrice;
}
