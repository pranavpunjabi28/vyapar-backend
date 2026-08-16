package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = DbFields.Tables.BUSINESS)
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("biz")
public class Business extends BaseEntity {
    private String name;
    private String legalName;
    private String phone;
    private String gstin;
    private String fssai;
    private String logoKey;
}
