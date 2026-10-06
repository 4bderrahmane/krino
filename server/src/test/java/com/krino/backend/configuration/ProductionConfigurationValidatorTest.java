package com.krino.backend.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigurationValidatorTest {

    private final ProductionConfigurationValidator validator = new ProductionConfigurationValidator();

    @Test
    void validate_safeProductionConfigurationDoesNotThrow() {
        assertThatCode(() -> validator.validate(safeEnvironment())).doesNotThrowAnyException();
    }

    @Test
    void validate_insecureProductionConfigurationThrowsHelpfulError() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.cookies.secure", "false")
                .withProperty("spring.jpa.hibernate.ddl-auto", "update")
                .withProperty("app.refresh-token.hmac-secret", "jwt-secret-value-padded-to-32-chars")
                .withProperty("app.cors.allowed-origins", "http://localhost:5173,*")
                .withProperty("app.storage.access-key", "minioadmin")
                .withProperty("app.mail.log-only", "true");

        assertThatThrownBy(() -> validator.validate(environment))
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

    @Test
    void validate_shortTokenSecretsAreRejected() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.authentication.secret", "too-short");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.authentication.secret must be at least 32 characters");
    }

    /** A prod.env.example copied verbatim must not boot. */
    @Test
    void validate_placeholderValuesFromTheExampleFileAreRejected() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("spring.datasource.password", "replace-with-real-db-password")
                .withProperty("app.storage.secret-key", "replace-with-minio-secret-key")
                .withProperty("app.mail.from", "no-reply@example.com")
                .withProperty("app.authentication.issuer", "https://api.example.com");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("spring.datasource.password still holds an example placeholder value")
                .hasMessageContaining("app.storage.secret-key still holds an example placeholder value")
                .hasMessageContaining("app.mail.from still holds an example placeholder value")
                .hasMessageContaining("app.authentication.issuer still holds an example placeholder value");
    }

    @Test
    void validate_frontendUrlMustBeAnExternalHttpsUrlWithoutTrailingSlash() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.frontend.url", "http://localhost:5000/");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.frontend.url must not end with a trailing slash")
                .hasMessageContaining("app.frontend.url must use HTTPS in production")
                .hasMessageContaining("app.frontend.url must not point at a local development host");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://krino-hr.io?source=mail",
            "https://krino-hr.io/#/welcome",
            "https://krino-hr.io?",
            "https://krino-hr.io#"
    })
    void validate_frontendUrlMustNotContainQueryOrFragment(String frontendUrl) {
        MockEnvironment environment = safeEnvironment().withProperty("app.frontend.url", frontendUrl);

        assertThatThrownBy(() -> validator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.frontend.url must not contain a query string or fragment");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://",
            "https:///krino-hr.io",
            "https://krino-hr.io/",
            "https://krino-hr.io/app",
            "https://user:password@krino-hr.io",
            "https://krino-hr.io?source=mail",
            "https://krino-hr.io#fragment",
            "https://krino-hr.io:65536"
    })
    void validate_malformedCorsOriginsAreRejected(String origin) {
        MockEnvironment environment = safeEnvironment().withProperty("app.cors.allowed-origins", origin);

        assertThatThrownBy(() -> validator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins must contain valid origins");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://localhost:5173", "https://127.0.0.1:5173", "https://[::1]:5173"})
    void validate_loopbackCorsOriginsAreRejected(String origin) {
        MockEnvironment environment = safeEnvironment().withProperty("app.cors.allowed-origins", origin);

        assertThatThrownBy(() -> validator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cors.allowed-origins must not include local development origins");
    }

    @ParameterizedTest
    @ValueSource(strings = {"*", "https://*.krino-hr.io"})
    void validate_wildcardCorsOriginsAreRejected(String origin) {
        MockEnvironment environment = safeEnvironment().withProperty("app.cors.allowed-origins", origin);

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.cors.allowed-origins must not include wildcard origins");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://krino-hr.io:8443",
            "https://localhost.krino-hr.io",
            "https://krino-hr.io, https://www.krino-hr.io:8443"
    })
    void validate_validExternalCorsOriginsAreAccepted(String origins) {
        MockEnvironment environment = safeEnvironment().withProperty("app.cors.allowed-origins", origins);

        assertThatCode(() -> validator.validate(environment)).doesNotThrowAnyException();
    }

    @Test
    void validate_legitimateValuesContainingTodoAreAccepted() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.mail.from-name", "Todo Recruiting")
                .withProperty("app.mail.from", "todor@krino-hr.io")
                .withProperty("app.authentication.secret", "jwt-secret-with-TODO-padded-to-32-chars");

        assertThatCode(() -> validator.validate(environment)).doesNotThrowAnyException();
    }

    @Test
    void validate_storageEndpointMustUseHttpsWhenItLeavesTheHost() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.storage.endpoint", "http://storage.krino-hr.io");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.storage.endpoint must use HTTPS in production unless it targets loopback");
    }

    /** MinIO on the same box never puts resume bytes on a network, so plain HTTP is fine there. */
    @ParameterizedTest
    @ValueSource(strings = {"http://127.0.0.1:9000", "http://localhost:9000", "http://[::1]:9000"})
    void validate_loopbackStorageEndpointMayUsePlainHttp(String endpoint) {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.storage.endpoint", endpoint);

        assertThatCode(() -> validator.validate(environment)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://127.0.0.1:9000", "ftp://localhost:9000", "wss://[::1]:9000"})
    void validate_loopbackStorageEndpointMustStillUseHttpOrHttps(String endpoint) {
        MockEnvironment environment = safeEnvironment().withProperty("app.storage.endpoint", endpoint);

        assertThatThrownBy(() -> validator.validate(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.storage.endpoint must use HTTP or HTTPS");
    }

    @Test
    void validate_mailSenderAndResendKeyAreChecked() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.mail.from", "not-an-email")
                .withProperty("resend.api-key", "replace-with-resend-api-key");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.mail.from must be a valid email address")
                .hasMessageContaining("resend.api-key does not look like a Resend key");
    }

    @Test
    void validate_adminBootstrapMustBeCompleteAndStrong() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.admin.email", "admin@krino-hr.io")
                .withProperty("app.admin.password", "short");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.admin.password must be at least 12 characters");
    }

    @Test
    void validate_halfConfiguredAdminBootstrapIsRejected() {
        MockEnvironment environment = safeEnvironment()
                .withProperty("app.admin.email", "admin@krino-hr.io");

        assertThatThrownBy(() -> validator.validate(environment))
                .hasMessageContaining("app.admin.email and app.admin.password must be set together");
    }

    /** Omitting both is the supported post-bootstrap state, not an error. */
    @Test
    void validate_absentAdminBootstrapIsAccepted() {
        assertThatCode(() -> validator.validate(safeEnvironment())).doesNotThrowAnyException();
    }

    @Test
    void onApplicationEvent_rejectsUnsafeConfigurationUnderTheProdProfile() {
        MockEnvironment environment = safeEnvironment().withProperty("app.cookies.secure", "false");
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> validator.onApplicationEvent(environmentPreparedEvent(environment)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.cookies.secure must be true");
    }

    /** Dev and test runs are allowed every shortcut this class exists to forbid. */
    @Test
    void onApplicationEvent_skipsValidationOutsideTheProdProfile() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.cookies.secure", "false")
                .withProperty("spring.jpa.hibernate.ddl-auto", "update");
        environment.setActiveProfiles("dev");

        assertThatCode(() -> validator.onApplicationEvent(environmentPreparedEvent(environment)))
                .doesNotThrowAnyException();
    }

    private ApplicationEnvironmentPreparedEvent environmentPreparedEvent(MockEnvironment environment) {
        return new ApplicationEnvironmentPreparedEvent(
                new DefaultBootstrapContext(),
                new SpringApplication(),
                new String[]{},
                environment);
    }

    private MockEnvironment safeEnvironment() {
        return new MockEnvironment()
                .withProperty("app.cookies.secure", "true")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("app.authentication.secret", "jwt-secret-value-padded-to-32-chars")
                .withProperty("app.refresh-token.hmac-secret", "refresh-secret-value-padded-to-32-chars")
                .withProperty("app.authentication.issuer", "https://api.krino-hr.io")
                .withProperty("app.cors.allowed-origins", "https://krino-hr.io,https://www.krino-hr.io")
                .withProperty("app.frontend.url", "https://krino-hr.io")
                .withProperty("app.storage.endpoint", "https://storage.krino-hr.io")
                .withProperty("app.storage.access-key", "krino-storage")
                .withProperty("app.storage.secret-key", "storage-secret-value")
                .withProperty("app.storage.bucket", "krino-cvs")
                .withProperty("app.storage.max-cv-size", "5MB")
                .withProperty("app.mail.from", "no-reply@krino-hr.io")
                .withProperty("app.mail.from-name", "Krino")
                .withProperty("resend.api-key", "re_test_key");
    }
}
