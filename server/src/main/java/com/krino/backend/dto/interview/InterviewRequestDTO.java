package com.krino.backend.dto.interview;

import com.krino.backend.entity.enums.InterviewRecommendation;
import com.krino.backend.entity.enums.InterviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequestDTO {

    // The application being interviewed. It determines the candidate and the job, so those
    // are not accepted from the client. Required on create and full update; on patch it is
    // optional but, when present, must match the interview's existing application (immutable).
    @NotNull(message = "Application ID cannot be null")
    private UUID applicationId;

    @NotNull(message = "Slot ID cannot be null")
    private UUID slotId;

    private InterviewStatus status;

    private String notes;

    // The interviewer's hiring signal. Required when status is COMPLETED and rejected for any
    // other status; the service enforces this against the interview's final state.
    private InterviewRecommendation recommendation;

    // Required on create and full update (PUT). PATCH skips bean validation, so a partial
    // update may omit it and keep the stored value.
    @NotNull(message = "isOnline must be specified")
    private Boolean isOnline;

    @Size(max = 512, message = "Meeting URL must not exceed 512 characters")
    private String meetingUrl;
}
