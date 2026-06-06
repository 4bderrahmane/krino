package com.jesa.interviewslotmanager.dto.application;

import com.jesa.interviewslotmanager.entity.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationUpdateDTO
{
    private Long jobId;
    private String resumeUrl;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}

