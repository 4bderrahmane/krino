package com.krino.backend.dto.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SlotRequestDTO
{
    // Optional: HR can create a slot on behalf of an interviewer.
    // When absent, the slot belongs to the authenticated user.
    private UUID interviewerId;

    @NotNull(message = "Interview date cannot be null")
    private LocalDate interviewDate;

    @NotNull(message = "Start time cannot be null")
    private LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalTime endTime;
}
