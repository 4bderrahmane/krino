package com.krino.backend.dto.application;

import com.krino.backend.entity.enums.ApplicationStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class ApplicationUpdateDTO
{
    private UUID jobId;
    private String resumeUrl;
    private ApplicationStatus status;
}

