package com.jesa.interviewslotmanager.dto.slot;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponseDTO
{

    private Long id;
    private Integer durationInMinutes;
    private boolean isAvailable = true;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
