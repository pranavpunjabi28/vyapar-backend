package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {DbFields.MEMBERSHIP_ID, DbFields.OUTLET_ID}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("outassign")
public class OutletAssignment extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Membership membership;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Outlet outlet;
}
