package com.bbu.vyaparbackend.shared;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @Column(length = 40, updatable = false, nullable = false)
    private String id;

    @Version
    private long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean archived;

    @PrePersist
    protected void assignId() {
        if (id != null) {
            return;
        }
        PrefixedId definition = getClass().getAnnotation(PrefixedId.class);
        if (definition == null) {
            throw new IllegalStateException("Persistent entity is missing @PrefixedId: " + getClass().getName());
        }
        id = PrefixedIdGenerator.generate(definition.value());
    }
}
