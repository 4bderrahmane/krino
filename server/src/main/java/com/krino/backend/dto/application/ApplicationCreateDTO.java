package com.krino.backend.dto.application;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateDTO
{
    @NotNull(message = "Job ID cannot be null")
    private UUID jobId;
    private String resumeUrl;
}