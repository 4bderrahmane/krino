package com.krino.backend.dto.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDTO
{
    private UUID jobId;
    private String resumeUrl;
    private LocalDateTime appliedAt;
}