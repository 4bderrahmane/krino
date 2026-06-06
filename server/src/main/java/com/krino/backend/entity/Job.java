package com.krino.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jobs")
@Entity
public class Job
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    @JsonBackReference
    private Department department;

    private String title;

    private String description;

    @Temporal(TemporalType.DATE)
    private Date applyingDeadline;

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
