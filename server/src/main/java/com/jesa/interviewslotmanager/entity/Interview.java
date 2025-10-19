package com.jesa.interviewslotmanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interviews")

public class Interview
{
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

    @OneToOne
    @JoinColumn(name = "slot_id")
    private Slot slot;

    @Lob
    private String notes;

    private Boolean isOnline;
}
