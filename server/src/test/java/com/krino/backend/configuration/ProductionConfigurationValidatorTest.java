package com.krino.backend.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTest {

    @Test
    void run_safeProductionConfigurationDoesNotThrow() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(safeEnvironment());

        validator.run(null);
    }

    @Test
    void run_insecureProductionConfigurationThrowsHelpfulError() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.cookies.secure", "false")
                .withProperty("spring.jpa.hibernate.ddl-auto", "update")
                .withProperty("app.refresh-token.hmac-secret", "jwt-secret-value")
                .withProperty("app.cors.allowed-origins", "http://localhost:5173,*")
                .withProperty("app.storage.access-key", "minioadmin")
                .withProperty("app.mail.log-only", "true");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

        assertThatThrownBy(() -> validator.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsafe production configuration")
                .hasMessageContaining("app.cookies.secure must be true")
                .hasMessageContaining("spring.jpa.hibernate.ddl-auto must not be update")
                .hasMessageContaining("JWT and refresh-token HMAC secrets must be different")
                .hasMessageContaining("must not include local development origins")
                .hasMessageContaining("must not include wildcard origins")
                .hasMessageContaining("MinIO storage credentials must not use default minioadmin values")
                .hasMessageContaining("app.mail.log-only must not be enabled in production");
    }

    private MockEnvironment safeEnvironment() {
        return new MockEnvironment()
                .withProperty("app.cookies.secure", "true")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("jwt.secret", "jwt-secret-value")
                .withProperty("app.refresh-token.hmac-secret", "refresh-secret-value")
                .withProperty("app.jwt.issuer", "https://api.example.com")
                .withProperty("app.cors.allowed-origins", "https://example.com,https://www.example.com")
                .withProperty("app.storage.endpoint", "https://storage.example.com")
                .withProperty("app.storage.access-key", "krino-storage")
                .withProperty("app.storage.secret-key", "storage-secret-value")
                .withProperty("app.storage.bucket", "krino-cvs")
                .withProperty("app.storage.max-cv-size", "5MB");
    }
}
