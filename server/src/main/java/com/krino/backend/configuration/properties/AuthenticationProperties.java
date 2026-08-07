package com.krino.backend.configuration.properties;

import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.authentication")
@GroupSequence({AuthenticationProperties.class, AuthenticationProperties.LifetimeChecks.class})
public record AuthenticationProperties(
        @NotBlank String issuer,
        @NotBlank @Size(min = 32) String secret,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl,
        @NotBlank String accessCookieName,
        @NotBlank String refreshCookieName
) {
    interface LifetimeChecks {
    }

    @AssertTrue(message = "app.authentication.access-ttl must be greater than zero", groups = LifetimeChecks.class)
    public boolean isAccessTtlPositive() {
        return !accessTtl.isZero() && !accessTtl.isNegative();
    }

    @AssertTrue(message = "app.authentication.refresh-ttl must be greater than zero", groups = LifetimeChecks.class)
    public boolean isRefreshTtlPositive() {
        return !refreshTtl.isZero() && !refreshTtl.isNegative();
    }

    @AssertTrue(message = "app.authentication.access-ttl must be shorter than app.authentication.refresh-ttl",
            groups = LifetimeChecks.class)
    public boolean isAccessTtlShorterThanRefreshTtl() {
        return accessTtl.compareTo(refreshTtl) < 0;
    }

    public Duration accessCookieMaxAge() {
        return accessTtl;
    }

    public Duration refreshCookieMaxAge() {
        return refreshTtl;
    }

    @Override
    public @NonNull String toString() {
        return """
                AuthenticationProperties[
                    issuer=%s,
                    accessTtl=%s,
                    refreshTtl=%s,
                    accessCookieName=%s,
                    refreshCookieName=%s,
                    secret=REDACTED
                ]
                """.formatted(
                issuer,
                accessTtl,
                refreshTtl,
                accessCookieName,
                refreshCookieName
        ).strip();
    }
}
