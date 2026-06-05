package com.jesa.interviewslotmanager.dto.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDTO
{
    private Long jobId;
    private String resumeUrl;
    private LocalDateTime appliedAt;
}