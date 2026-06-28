package com.krino.backend.entity;

import com.krino.backend.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "applications",
        indexes = {
                @Index(name = "idx_applications_candidate", columnList = "user_id")
        }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_applications_job_candidate", columnNames = {"job_id", "user_id"})
        }
)
public class Application extends AbstractPublicEntity {

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false, foreignKey = @ForeignKey(name = "fk_applications_job"))
    private Job job;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_applications_candidate"))
    private User candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(length = 512)
    private String resumeObjectKey;

    private String resumeOriginalFilename;

    @Column(length = 100)
    private String resumeContentType;

    private Long resumeSizeBytes;

    private LocalDateTime resumeUploadedAt;

    // Domain event — when the candidate applied. Distinct from the base's
    // createdDate audit column (which they coincide with today, but appliedAt is
    // the business-meaningful timestamp exposed to clients).
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    // Prevents silent lost updates from concurrent edits.
    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

}
