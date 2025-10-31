package com.jesa.interviewslotmanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
//import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

//@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "applications")
public class Application
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    private String resumeUrl;

    private LocalDateTime appliedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}