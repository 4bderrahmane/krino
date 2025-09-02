package com.jesa.interviewslotmanager.DTO.Slot;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SlotRequestDTO
{

    @NotBlank(message = "Duration in minutes cannot be blank")
    private String durationInMinutes;

    @NotNull(message = "Interview date cannot be null")
    private LocalDate interviewDate;

    @NotNull(message = "Start time cannot be null")
    private LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalTime endTime;
}
