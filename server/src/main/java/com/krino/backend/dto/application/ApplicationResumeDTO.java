package com.krino.backend.dto.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResumeDTO {
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private Instant uploadedAt;
}
