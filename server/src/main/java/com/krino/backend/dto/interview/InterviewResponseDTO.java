package com.krino.backend.dto.interview;

import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.enums.InterviewStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class InterviewResponseDTO {

    private UUID id;
    private UserResponseDTO interviewer;
    private UserResponseDTO candidate;
    private JobResponseDTO job;
    private SlotResponseDTO slot;
    private InterviewStatus status;
    private String notes;
    private Boolean isOnline;
    private String meetingUrl;

}
