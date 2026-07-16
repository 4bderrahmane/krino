package com.krino.backend.service.resume;

import com.krino.backend.configuration.properties.StorageProperties;
import com.krino.backend.dto.user.UserRegistrationDTO;
import com.krino.backend.repository.UserRepository;
import com.krino.backend.service.AuthenticationService;
import com.krino.backend.support.AbstractIntegrationTest;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class RegistrationResumeRollbackIntegrationTest extends AbstractIntegrationTest {

    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    private final PlatformTransactionManager transactionManager;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    @Test
    void registrationRollbackDeletesUploadedResume() {
        String email = "rollback-" + UUID.randomUUID() + "@test.local";
        UserRegistrationDTO request = new UserRegistrationDTO(
                "Test",
                "User",
                email,
                "Password123!",
                "123456789");
        MockMultipartFile resume = new MockMultipartFile(
                "resume",
                "cv.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII));
        AtomicReference<String> objectKey = new AtomicReference<>();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            authenticationService.register(request, resume);
            userRepository.flush();
            objectKey.set(userRepository.findByEmail(email).orElseThrow().getResumeObjectKey());
            status.setRollbackOnly();
        });

        assertThat(userRepository.findByEmail(email)).isEmpty();
        assertThatThrownBy(() -> minioClient.statObject(StatObjectArgs.builder()
                .bucket(storageProperties.getBucket())
                .object(objectKey.get())
                .build()))
                .isInstanceOf(ErrorResponseException.class)
                .satisfies(exception -> assertThat(((ErrorResponseException) exception)
                        .errorResponse().code()).isEqualTo("NoSuchKey"));
    }
}
