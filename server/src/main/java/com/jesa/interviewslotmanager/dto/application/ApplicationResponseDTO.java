package com.jesa.interviewslotmanager.dto.application;

import com.jesa.interviewslotmanager.dto.user.UserResponseDTO;
import com.jesa.interviewslotmanager.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDTO
{
    private Long id;
    private Long jobId;
    private UserResponseDTO candidate;
    private ApplicationStatus status;
    private String resumeUrl;
    private LocalDateTime appliedAt;
}

