package com.jesa.interviewslotmanager.dto.Application;

import com.jesa.interviewslotmanager.dto.Job.JobResponseDTO;
import com.jesa.interviewslotmanager.dto.User.UserResponseDTO;
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
