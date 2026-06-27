package com.krino.backend.entity;

import com.krino.backend.entity.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

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
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

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

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    void ensurePublicId() {
        if (publicId == null) publicId = UUID.randomUUID();
    }

}
