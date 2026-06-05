package com.jesa.interviewslotmanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "slots")
public class Slot
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    private Integer durationInMinutes;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Setter(AccessLevel.NONE)
    @OneToOne(mappedBy = "slot")
    private Interview interview;

    public void setInterview( Interview interview)
    {
        this.interview = interview;
        this.available = (interview == null);
    }
}