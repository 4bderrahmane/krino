package com.krino.backend.service.resume;

import java.io.InputStream;

public record ResumeDownload(
        String originalFilename,
        String contentType,
        Long sizeBytes,
        InputStream inputStream
) {
}
