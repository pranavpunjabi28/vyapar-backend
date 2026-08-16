package com.bbu.vyaparbackend.business;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("invite")
public class StaffInvitation extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Business business;
    @Column(nullable = false, length = 320)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    @Column(nullable = false)
    private Instant expiresAt;
    private Instant acceptedAt;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = DbFields.Tables.STAFF_INVITATION_OUTLET,
            joinColumns = @JoinColumn(name = DbFields.INVITATION_ID))
    @Column(name = DbFields.OUTLET_ID, nullable = false)
    private Set<String> outletIds = new HashSet<>();
}
