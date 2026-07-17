package com.krino.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base for every persistent entity: a generated {@code Long} surrogate key plus
 * Spring Data "who + when" auditing. The id is a concrete {@code Long} (all
 * entities use it), so there is no type parameter to thread through.
 *
 * <p>Auditing is populated by {@link AuditingEntityListener}; the "who" columns
 * come from the {@code AuditorAware} bean (see JpaAuditingConfiguration), so
 * {@code @EnableJpaAuditing} must be active or they stay null. The
 * {@link ColumnDefault}s let Hibernate ddl-auto add these NOT NULL columns to
 * already-populated tables, backfilling existing rows instead of failing.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "createdBy", "createdDate", "lastModifiedBy", "lastModifiedDate" }, allowGetters = true)
public abstract class AbstractAuditingEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedBy
    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    @ColumnDefault("'system'")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", nullable = false, length = 100)
    @ColumnDefault("'system'")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date", nullable = false)
    @ColumnDefault("CURRENT_TIMESTAMP")
    private Instant lastModifiedDate;
}
