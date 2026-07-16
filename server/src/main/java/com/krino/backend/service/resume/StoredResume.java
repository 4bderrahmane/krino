package com.krino.backend.service.resume;

import java.time.Instant;

public record StoredResume(
        String objectKey,
        String originalFilename,
        String contentType,
        long sizeBytes,
        Instant uploadedAt
) {}