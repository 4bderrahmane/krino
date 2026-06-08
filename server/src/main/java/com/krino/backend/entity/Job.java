package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_public_id", columnList = "public_id")
})
@Entity
public class Job
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "public_id", unique = true, nullable = false, updatable = false,
            columnDefinition = "VARCHAR(36)")
    private UUID publicId;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    @JsonBackReference
    private Department department;

    private String title;

    private String description;

    private LocalDate applyingDeadline;

    @Column(nullable = false)
    private JobType type;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    public enum JobStatus
    {
        DRAFT, OPEN, CLOSED, FILLED
    }

    public enum JobType
    {
        FULL_TIME, PART_TIME, INTERNSHIP, CONTRACT
    }
}
