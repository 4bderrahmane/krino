package com.krino.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.Hibernate;

import java.util.UUID;

/**
 * Base for entities exposed through the API by a stable, non-enumerable public
 * id. The internal {@code Long} key never leaves the server; clients reference
 * the {@code publicId} instead. Equality is by {@code publicId} (assigned at
 * construction), which is Hibernate-proxy safe and stable across the entity
 * lifecycle, unlike the generated id.
 */
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class AbstractPublicEntity extends AbstractAuditingEntity {

    @Builder.Default
    @Column(name = "public_id", unique = true, nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID publicId = UUID.randomUUID();

    @PrePersist
    void ensurePublicId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;

        AbstractPublicEntity that = (AbstractPublicEntity) other;
        return publicId != null && publicId.equals(that.getPublicId());
    }

    @Override
    public int hashCode() {
        return publicId != null ? publicId.hashCode() : 0;
    }
}
