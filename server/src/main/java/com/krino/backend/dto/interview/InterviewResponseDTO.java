package com.jesa.interviewslotmanager.dto.interview;

import com.jesa.interviewslotmanager.dto.job.JobResponseDTO;
import com.jesa.interviewslotmanager.dto.slot.SlotResponseDTO;
import com.jesa.interviewslotmanager.dto.user.UserResponseDTO;
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