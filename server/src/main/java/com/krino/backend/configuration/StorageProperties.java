package com.krino.backend.configuration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {
    private static final long MAX_ALLOWED_CV_SIZE_BYTES = DataSize.ofMegabytes(5).toBytes();

    @NotBlank private String endpoint;

    @NotBlank private String accessKey;

    @NotBlank private String secretKey;

    @NotBlank private String bucket;

    @NotNull private DataSize maxCvSize;

    @AssertTrue(message = "app.storage.max-cv-size must be 5MB or less")
    public boolean isMaxCvSizeAllowed() {
        return maxCvSize != null && maxCvSize.toBytes() > 0 && maxCvSize.toBytes() <= MAX_ALLOWED_CV_SIZE_BYTES;
    }
}
