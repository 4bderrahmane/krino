package com.krino.backend.dto.interview;

import com.krino.backend.dto.job.JobResponseDTO;
import com.krino.backend.dto.slot.SlotResponseDTO;
import com.krino.backend.dto.user.UserResponseDTO;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class InterviewResponseDTO
{

    private Long id;
    private UserResponseDTO interviewer;
    private UserResponseDTO candidate;
    private JobResponseDTO job;
    private SlotResponseDTO slot;
    private String notes;
    private Boolean isOnline;

}