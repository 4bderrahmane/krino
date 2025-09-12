package com.jesa.interviewslotmanager.dto.application;

import com.jesa.interviewslotmanager.dto.job.JobResponseDTO;
import com.jesa.interviewslotmanager.dto.user.UserResponseDTO;
import com.jesa.interviewslotmanager.entity.ApplicationStatus;
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
