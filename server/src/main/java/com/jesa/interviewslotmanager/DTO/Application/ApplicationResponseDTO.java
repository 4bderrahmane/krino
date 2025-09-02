package com.jesa.interviewslotmanager.DTO.Application;

import com.jesa.interviewslotmanager.DTO.Job.JobResponseDTO;
import com.jesa.interviewslotmanager.DTO.User.UserResponseDTO;
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
