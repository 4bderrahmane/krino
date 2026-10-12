package com.krino.backend.dto.interview;

import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.slot.SlotSummaryDTO;
import com.krino.backend.dto.user.UserSummaryDTO;
import com.krino.backend.entity.enums.InterviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public final class CandidateInterviewResponseDTO implements InterviewView {

    private UUID id;
    private UUID applicationId;
    private UserSummaryDTO interviewer;
    private JobResponseDTO job;
    private SlotSummaryDTO slot;
    private InterviewStatus status;
    private Boolean isOnline;
    private String meetingUrl;
}
