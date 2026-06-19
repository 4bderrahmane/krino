package com.krino.backend.dto.interview;

import com.krino.backend.entity.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequestDTO
{
    @NotNull(message = "Interviewer ID cannot be null")
    private UUID interviewerId;

    @NotNull(message = "Candidate ID cannot be null")
    private UUID candidateId;

    @NotNull(message = "Job ID cannot be null")
    private UUID jobId;

    @NotNull(message = "Slot ID cannot be null")
    private UUID slotId;

    private InterviewStatus status;

    private String notes;

    private Boolean isOnline;

    private String meetingUrl;
}
