package com.krino.backend.dto.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponseDTO
{

    private UUID id;
    private Integer durationInMinutes;
    private boolean available = true;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
