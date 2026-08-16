package com.bbu.vyaparbackend.auth;

import com.bbu.vyaparbackend.shared.BaseEntity;
import com.bbu.vyaparbackend.shared.DbFields;
import com.bbu.vyaparbackend.shared.PrefixedId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = DbFields.Tables.APP_USER)
@Getter
@Setter
@NoArgsConstructor
@PrefixedId("user")
public class User extends BaseEntity {
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(nullable = false)
    private String passwordHash;
    @Column(nullable = false, length = 120)
    private String displayName;
    @Column(nullable = false)
    private boolean passwordChangeRequired;
}
