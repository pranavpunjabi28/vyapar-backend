package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.auth.User;
import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {DbFields.BUSINESS_ID, DbFields.USER_ID}))
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("member")
public class Membership extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Business business;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
