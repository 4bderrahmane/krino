package com.krino.backend.configuration.properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationPropertiesTest {
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nullLifetimeIsHandledByNotNullWithoutManualConstructorChecks() {
        AuthenticationProperties properties = new AuthenticationProperties(
                "krino-test",
                SECRET,
                null,
                Duration.ofDays(30),
                "access_token",
                "refresh_token"
        );

        Set<ConstraintViolation<AuthenticationProperties>> violations = validator.validate(properties);

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath().toString()).isEqualTo("accessTtl");
            assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(NotNull.class);
        });
    }

    @Test
    void nonPositiveLifetimeIsRejectedAfterNullValidationPasses() {
        AuthenticationProperties properties = properties(Duration.ZERO, Duration.ofDays(30));

        assertThat(validator.validate(properties))
                .extracting(ConstraintViolation::getMessage)
                .contains("app.authentication.access-ttl must be greater than zero");
    }

    @Test
    void accessLifetimeMustBeShorterThanRefreshLifetime() {
        AuthenticationProperties properties = properties(Duration.ofDays(30), Duration.ofDays(30));

        assertThat(validator.validate(properties))
                .extracting(ConstraintViolation::getMessage)
                .contains("app.authentication.access-ttl must be shorter than app.authentication.refresh-ttl");
    }

    @Test
    void validPropertiesHaveNoConstraintViolationsAndRedactSecret() {
        AuthenticationProperties properties = properties(Duration.ofMinutes(15), Duration.ofDays(30));

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.toString())
                .contains("secret=REDACTED")
                .doesNotContain(SECRET);
    }

    private AuthenticationProperties properties(Duration accessTtl, Duration refreshTtl) {
        return new AuthenticationProperties(
                "krino-test",
                SECRET,
                accessTtl,
                refreshTtl,
                "access_token",
                "refresh_token"
        );
    }
}
