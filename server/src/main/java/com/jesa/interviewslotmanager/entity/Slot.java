package com.jesa.interviewslotmanager.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private Integer durationInMinutes;

    @Column(nullable = false)
    private boolean isAvailable = true;

    private LocalDate interviewDate;

    private LocalTime startTime;

    private LocalTime endTime;
}
