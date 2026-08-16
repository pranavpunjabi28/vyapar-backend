package com.bbu.vyaparbackend.customer;

import com.bbu.vyaparbackend.business.Business;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {@Index(name = DbFields.Indexes.CUSTOMER_BUSINESS_PHONE,
        columnList = DbFields.BUSINESS_ID + "," + DbFields.PHONE)})
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("customer")
public class Customer extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Business business;
    @Column(nullable = false)
    private String name;
    private String phone;
    private String email;
    @Column(length = 1000)
    private String address;
}
