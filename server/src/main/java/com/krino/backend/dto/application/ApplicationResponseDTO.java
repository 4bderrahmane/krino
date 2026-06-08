package com.krino.backend.dto.application;

import com.krino.backend.dto.user.UserResponseDTO;
import com.krino.backend.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponseDTO
{
    private UUID id;
    private UUID jobId;
    private UserResponseDTO candidate;
    private ApplicationStatus status;
    private String resumeUrl;
    private LocalDateTime appliedAt;
}

