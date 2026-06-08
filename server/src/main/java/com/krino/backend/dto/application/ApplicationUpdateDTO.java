package com.krino.backend.dto.application;

import com.krino.backend.entity.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ApplicationUpdateDTO
{
    private UUID jobId;
    private String resumeUrl;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
}

