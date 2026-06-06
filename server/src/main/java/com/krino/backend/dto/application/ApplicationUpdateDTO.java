package com.krino.backend.dto.application;

import com.krino.backend.entity.ApplicationStatus;
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

