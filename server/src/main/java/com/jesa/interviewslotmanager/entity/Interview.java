package com.jesa.interviewslotmanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interviews")

public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "interviewer_id")
    private User interviewer;

    @ManyToOne
    @JoinColumn(name = "candidate_id")
    private User candidate;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @Temporal(TemporalType.DATE)
    private Date interviewDate;

    @Temporal(TemporalType.TIME)
    private Date startTime;

    @Temporal(TemporalType.TIME)
    private Date endTime;

    private boolean isOnline = false;

}
