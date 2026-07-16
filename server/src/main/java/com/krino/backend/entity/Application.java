package com.krino.backend.entity;

import com.krino.backend.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
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

    private Instant resumeUploadedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    @Version
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private long version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Application)) return false;
        Long id = getId();
        return id != null && id.equals(((Application) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
