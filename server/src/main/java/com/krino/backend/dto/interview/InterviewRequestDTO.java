package com.krino.backend.dto.interview;

import com.krino.backend.entity.enums.InterviewRecommendation;
import com.krino.backend.entity.enums.InterviewStatus;
import com.krino.backend.validation.ValidationGroups;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;
import lombok.*;

import java.util.UUID;

// Create (POST) and full update (PUT) validate the FullUpdate group, so the @NotNull fields
// are required there. PATCH validates the Default group only, so those fields may be omitted
// (null means "unchanged") while null-tolerant constraints like @Size still apply.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRequestDTO {

    // The application being interviewed. It determines the candidate and the job, so those
    // are not accepted from the client. Required on create and full update; on patch it is
    // optional but, when present, must match the interview's existing application (immutable).
    @NotNull(message = "Application ID cannot be null", groups = ValidationGroups.FullUpdate.class)
    private UUID applicationId;

    @NotNull(message = "Slot ID cannot be null", groups = ValidationGroups.FullUpdate.class)
    private UUID slotId;

    private InterviewStatus status;

    private String notes;

    // The interviewer's hiring signal. Required when status is COMPLETED and rejected for any
    // other status; the service enforces this against the interview's final state.
    private InterviewRecommendation recommendation;

    @NotNull(message = "isOnline must be specified", groups = ValidationGroups.FullUpdate.class)
    private Boolean isOnline;

    @Size(max = 512, message = "Meeting URL must not exceed 512 characters",
            groups = {Default.class, ValidationGroups.FullUpdate.class})
    private String meetingUrl;
}
