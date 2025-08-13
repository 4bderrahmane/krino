package com.InterviewManager.interview_slot_manager.DTO.Application;

import com.InterviewManager.interview_slot_manager.DTO.Job.JobResponseDTO;
import com.InterviewManager.interview_slot_manager.DTO.User.UserResponseDTO;
import com.InterviewManager.interview_slot_manager.entity.ApplicationStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class ApplicationResponseDTO {

    private Long id;
    private ApplicationStatus status;
    private JobResponseDTO job;
    private UserResponseDTO applicant;
}
