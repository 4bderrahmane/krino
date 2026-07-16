package com.krino.backend.service.resume;

import com.krino.backend.configuration.properties.StorageProperties;
import com.krino.backend.exception.FileStorageException;
import com.krino.backend.utility.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.SourceObject;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeStorageService {
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final Clock clock;

    private volatile boolean bucketReady;

    public StoredResume uploadResume(UUID applicationPublicId, MultipartFile file) {
        return store(buildObjectKey("applications", applicationPublicId), file);
    }

    public StoredResume uploadUserResume(UUID userPublicId, MultipartFile file) {
        return store(buildObjectKey("users", userPublicId), file);
    }

    private StoredResume store(String objectKey, MultipartFile file) {
        validateResume(file);

        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1L)
                    .contentType(PDF_CONTENT_TYPE)
                    .build());
        } catch (Exception ex) {
            throw new FileStorageException(ErrorCode.EXTERNAL_SERVICE_FAILURE, "Could not store the resume file.", ex);
        }

        return new StoredResume(
                objectKey,
                normalizeOriginalFilename(file.getOriginalFilename()),
                PDF_CONTENT_TYPE,
                file.getSize(),
                Instant.now(clock));
    }

    /**
     * Server-side copy of an already-stored résumé (e.g., a user's base CV) into a new
     * application-scoped object. The source is a previously validated PDF, so no
     * re-validation is needed. Returns the new object key.
     */
    public String copyResumeForApplication(String sourceObjectKey, UUID applicationPublicId) {
        requireObjectKey(sourceObjectKey);
        String targetObjectKey = buildObjectKey("applications", applicationPublicId);
        try {
            ensureBucketExists();
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(targetObjectKey)
                    .source(SourceObject.builder()
                            .bucket(storageProperties.getBucket())
                            .object(sourceObjectKey)
                            .build())
                    .build());
        } catch (Exception ex) {
            throw new FileStorageException(ErrorCode.EXTERNAL_SERVICE_FAILURE, "Could not copy the resume file.", ex);
        }
        return targetObjectKey;
    }

    public InputStream downloadResume(String objectKey) {
        requireObjectKey(objectKey);
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new FileStorageException(ErrorCode.EXTERNAL_SERVICE_FAILURE, "Could not read the resume file.", ex);
        }
    }

    public void deleteResume(String objectKey) {
        requireObjectKey(objectKey);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new FileStorageException(ErrorCode.EXTERNAL_SERVICE_FAILURE, "Could not delete the resume file.", ex);
        }
    }

    public void deleteResumeBestEffort(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        try {
            deleteResume(objectKey);
        } catch (FileStorageException ex) {
            log.warn("Could not delete obsolete resume object key={}", objectKey, ex);
        }
    }

    private void validateResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException(ErrorCode.VALIDATION_ERROR, "Resume file cannot be empty.");
        }

        long maxSizeBytes = storageProperties.getMaxCvSize().toBytes();
        if (file.getSize() > maxSizeBytes) {
            throw new FileStorageException(ErrorCode.PAYLOAD_TOO_LARGE, "Resume file cannot exceed " + storageProperties.getMaxCvSize() + ".");
        }

        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(file.getContentType())) {
            throw new FileStorageException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Only PDF resume files are supported.");
        }

        if (!hasPdfSignature(file)) {
            throw new FileStorageException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Resume file must be a valid PDF.");
        }
    }

    private boolean hasPdfSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(PDF_SIGNATURE.length);
            return Arrays.equals(PDF_SIGNATURE, signature);
        } catch (IOException ex) {
            throw new FileStorageException(ErrorCode.VALIDATION_ERROR, "Could not read the resume file.", ex);
        }
    }

    private void ensureBucketExists() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                        .bucket(storageProperties.getBucket())
                        .build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder()
                            .bucket(storageProperties.getBucket())
                            .build());
                }
                bucketReady = true;
            } catch (Exception ex) {
                throw new FileStorageException(ErrorCode.EXTERNAL_SERVICE_FAILURE, "Could not prepare resume storage.", ex);
            }
        }
    }

    private String buildObjectKey(String ownerPrefix, UUID ownerPublicId) {
        return ownerPrefix + "/" + ownerPublicId + "/resume/" + UUID.randomUUID() + ".pdf";
    }

    private String normalizeOriginalFilename(String originalFilename) {
        String cleaned = StringUtils.cleanPath(Objects.toString(originalFilename, "").trim());
        int lastSeparator = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf('\\'));
        if (lastSeparator >= 0) {
            cleaned = cleaned.substring(lastSeparator + 1);
        }
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..")) {
            return "resume.pdf";
        }
        if (cleaned.length() > 255) {
            return cleaned.substring(cleaned.length() - 255);
        }
        return cleaned;
    }

    private void requireObjectKey(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            throw new FileStorageException(ErrorCode.RESOURCE_NOT_FOUND, "Resume file not found.");
        }
    }

}
