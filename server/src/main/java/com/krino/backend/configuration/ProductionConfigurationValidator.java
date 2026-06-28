package com.krino.backend.configuration;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionConfigurationValidator implements ApplicationRunner {
    private static final Set<String> UNSAFE_DDL_MODES = Set.of("create", "create-drop", "update");

    private final Environment environment;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        requireTrue("app.cookies.secure", errors);
        rejectUnsafeDdlMode(errors);
        requireNonBlank("jwt.secret", errors);
        requireNonBlank("app.refresh-token.hmac-secret", errors);
        rejectSharedTokenSecrets(errors);
        requireNonBlank("app.jwt.issuer", errors);
        validateCorsOrigins(errors);
        requireNonBlank("app.storage.endpoint", errors);
        requireNonBlank("app.storage.access-key", errors);
        requireNonBlank("app.storage.secret-key", errors);
        requireNonBlank("app.storage.bucket", errors);
        requireNonBlank("app.storage.max-cv-size", errors);
        rejectDefaultStorageCredentials(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration:\n - " + String.join("\n - ", errors));
        }
    }

    private void requireTrue(String property, List<String> errors) {
        if (!environment.getProperty(property, Boolean.class, false)) {
            errors.add(property + " must be true in production");
        }
    }

    private void rejectUnsafeDdlMode(List<String> errors) {
        String ddlMode = environment.getProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .toLowerCase(Locale.ROOT);
        if (UNSAFE_DDL_MODES.contains(ddlMode)) {
            errors.add("spring.jpa.hibernate.ddl-auto must not be " + ddlMode + " in production");
        }
    }

    private void requireNonBlank(String property, List<String> errors) {
        if (!StringUtils.hasText(environment.getProperty(property))) {
            errors.add(property + " must be set in production");
        }
    }

    private void rejectSharedTokenSecrets(List<String> errors) {
        String jwtSecret = environment.getProperty("jwt.secret");
        String refreshSecret = environment.getProperty("app.refresh-token.hmac-secret");
        if (StringUtils.hasText(jwtSecret) && jwtSecret.equals(refreshSecret)) {
            errors.add("JWT and refresh-token HMAC secrets must be different in production");
        }
    }

    private void rejectDefaultStorageCredentials(List<String> errors) {
        String accessKey = environment.getProperty("app.storage.access-key");
        String secretKey = environment.getProperty("app.storage.secret-key");
        if ("minioadmin".equals(accessKey) || "minioadmin".equals(secretKey)) {
            errors.add("MinIO storage credentials must not use default minioadmin values in production");
        }
    }

    private void validateCorsOrigins(List<String> errors) {
        String configuredOrigins = environment.getProperty("app.cors.allowed-origins");
        if (!StringUtils.hasText(configuredOrigins)) {
            errors.add("app.cors.allowed-origins must be set in production");
            return;
        }

        List<String> origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();

        if (origins.isEmpty()) {
            errors.add("app.cors.allowed-origins must include at least one production origin");
        }

        for (String origin : origins) {
            String normalizedOrigin = origin.toLowerCase(Locale.ROOT);
            if ("*".equals(origin)) {
                errors.add("app.cors.allowed-origins must not include wildcard origins in production");
            }
            if (normalizedOrigin.contains("localhost") || normalizedOrigin.contains("127.0.0.1")) {
                errors.add("app.cors.allowed-origins must not include local development origins in production");
            }
            if (!normalizedOrigin.startsWith("https://")) {
                errors.add("app.cors.allowed-origins must use HTTPS origins in production: " + origin);
            }
        }
    }
}
