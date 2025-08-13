package com.InterviewManager.interview_slot_manager.DTO.Interview;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class InterviewRequestDTO {
    @NotNull(message = "Interviewer ID cannot be null")
    private Long interviewerId;

    @NotNull(message = "Candidate ID cannot be null")
    private Long candidateId;

    @NotNull(message = "Job ID cannot be null")
    private Long jobId;

    @NotNull(message = "Interview date cannot be null")
    private LocalDate interviewDate;

    @NotNull(message = "Start time cannot be null")
    private LocalTime startTime;

    @NotNull(message = "End time cannot be null")
    private LocalTime endTime;

    private boolean isOnline = false;
}
