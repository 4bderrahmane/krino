package com.InterviewManager.interview_slot_manager.DTO.Slot;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotResponseDTO {

    private Long id;
    private String durationInMinutes;
    private boolean isAvailable = true;
    private LocalDate interviewDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
