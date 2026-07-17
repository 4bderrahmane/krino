package com.krino.backend.service.resume;

import com.krino.backend.configuration.properties.StorageProperties;
import com.krino.backend.exception.FileStorageException;
import com.krino.backend.utility.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResumeStorageServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-24T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void uploadResume_validPdfUploadsToMinioAndReturnsMetadata() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ResumeStorageService storageService =
                new ResumeStorageService(minioClient, storageProperties(), FIXED_CLOCK, eventPublisher);
        UUID applicationId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "../candidate-cv.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        StoredResume storedResume = storageService.uploadResume(applicationId, file);

        assertThat(storedResume.objectKey()).startsWith("applications/" + applicationId + "/resume/");
        assertThat(storedResume.objectKey()).endsWith(".pdf");
        assertThat(storedResume.originalFilename()).isEqualTo("candidate-cv.pdf");
        assertThat(storedResume.contentType()).isEqualTo("application/pdf");
        assertThat(storedResume.sizeBytes()).isEqualTo(file.getSize());
        assertThat(storedResume.uploadedAt()).isEqualTo(FIXED_CLOCK.instant());
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(eventPublisher).publishEvent(new ResumeStoredEvent(storedResume.objectKey()));
    }

    @Test
    void uploadUserResume_validPdfUsesUserScopedKey() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ResumeStorageService storageService =
                new ResumeStorageService(minioClient, storageProperties(), FIXED_CLOCK, eventPublisher);
        UUID userId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "base-cv.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));

        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        StoredResume storedResume = storageService.uploadUserResume(userId, file);

        assertThat(storedResume.objectKey()).startsWith("users/" + userId + "/resume/");
        assertThat(storedResume.objectKey()).endsWith(".pdf");
        assertThat(storedResume.originalFilename()).isEqualTo("base-cv.pdf");
        verify(minioClient).putObject(any(PutObjectArgs.class));
        verify(eventPublisher).publishEvent(new ResumeStoredEvent(storedResume.objectKey()));
    }

    @Test
    void copyResumeForApplication_publishesStoredEventForRollbackCleanup() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ResumeStorageService storageService =
                new ResumeStorageService(minioClient, storageProperties(), FIXED_CLOCK, eventPublisher);
        UUID applicationId = UUID.randomUUID();
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String objectKey = storageService.copyResumeForApplication("users/user-id/resume/base.pdf", applicationId);

        assertThat(objectKey).startsWith("applications/" + applicationId + "/resume/");
        verify(minioClient).copyObject(any(CopyObjectArgs.class));
        verify(eventPublisher).publishEvent(new ResumeStoredEvent(objectKey));
    }

    @Test
    void deleteResumeAfterCommit_publishesDeletionRequestWithoutDeletingImmediately() {
        MinioClient minioClient = mock(MinioClient.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        ResumeStorageService storageService =
                new ResumeStorageService(minioClient, storageProperties(), FIXED_CLOCK, eventPublisher);
        String objectKey = "applications/application-id/resume/cv.pdf";

        storageService.deleteResumeAfterCommit(objectKey);

        verify(eventPublisher).publishEvent(new ResumeDeletionRequestedEvent(objectKey));
        verifyNoInteractions(minioClient);
    }

    @Test
    void uploadResume_oversizedFileThrowsPayloadTooLarge() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(DataSize.ofMegabytes(5).toBytes() + 1);

        Throwable thrown = catchThrowable(() -> storageService().uploadResume(UUID.randomUUID(), file));

        assertThat(thrown).isInstanceOf(FileStorageException.class);
        assertThat(((FileStorageException) thrown).getErrorCode()).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    void uploadResume_nonPdfContentTypeThrowsUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "candidate.txt",
                "text/plain",
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));

        Throwable thrown = catchThrowable(() -> storageService().uploadResume(UUID.randomUUID(), file));

        assertThat(thrown).isInstanceOf(FileStorageException.class);
        assertThat(((FileStorageException) thrown).getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void uploadResume_wrongSignatureThrowsUnsupportedMediaType() {
        MockMultipartFile file = new MockMultipartFile(
                "resume",
                "candidate.pdf",
                "application/pdf",
                "not a pdf".getBytes(StandardCharsets.US_ASCII));

        Throwable thrown = catchThrowable(() -> storageService().uploadResume(UUID.randomUUID(), file));

        assertThat(thrown).isInstanceOf(FileStorageException.class);
        assertThat(((FileStorageException) thrown).getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    private ResumeStorageService storageService() {
        return new ResumeStorageService(
                mock(MinioClient.class),
                storageProperties(),
                FIXED_CLOCK,
                mock(ApplicationEventPublisher.class));
    }

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("minioadmin");
        properties.setSecretKey("minioadmin");
        properties.setBucket("krino-test-cvs");
        properties.setMaxCvSize(DataSize.ofMegabytes(5));
        return properties;
    }
}
