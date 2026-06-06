package com.krino.backend.dto.interview;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequestDTO
{
    @NotNull(message = "Interviewer ID cannot be null")
    private Long interviewerId;

    @NotNull(message = "Candidate ID cannot be null")
    private Long candidateId;

    @NotNull(message = "Job ID cannot be null")
    private Long jobId;

    @NotNull(message = "Slot ID cannot be null")
    private Long slotId;

    private String notes;

    private Boolean isOnline;
}
