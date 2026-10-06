package com.krino.backend.configuration;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Refuses to let the application start under the {@code prod} profile with a configuration that
 * would be unsafe in production.
 *
 * <p>This runs on {@link ApplicationEnvironmentPreparedEvent}, not as an {@code ApplicationRunner},
 * and that timing is the point. Runners fire after the context has been refreshed, by which time
 * Hibernate has already built its {@code SessionFactory} and applied whatever {@code ddl-auto} told
 * it to: rejecting {@code update} at that stage reports a schema mutation that has already
 * happened. The environment-prepared event fires after config data (profiles, {@code
 * application-prod.yaml}, environment variables) is fully resolved but before the context is
 * created, so a rejection here stops the process while the database is still untouched.
 *
 * <p>It is registered explicitly in {@link com.krino.backend.KrinoApplication}. That also keeps it
 * out of {@code @SpringBootTest} contexts, which build their own {@code SpringApplication} and have
 * no business running production checks.
 */
public class ProductionConfigurationValidator
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    private static final String PROD_PROFILE = "prod";
    private static final Set<String> UNSAFE_DDL_MODES = Set.of("create", "create-drop", "update");
    private static final int MINIMUM_SECRET_LENGTH = 32;
    private static final int MINIMUM_ADMIN_PASSWORD_LENGTH = 12;

    /**
     * Markers used by example configuration values. Matched
     * case-insensitively against every property in {@link #PLACEHOLDER_SCANNED_PROPERTIES}, so a
     * {@code prod.env.example} copied verbatim fails loudly instead of booting with example values.
     */
    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "replace-with",
            "replace_with",
            "change-me",
            "changeme",
            "placeholder",
            "example.com",
            "example.org",
            "example.net",
            "your-value");

    private static final List<String> PLACEHOLDER_SCANNED_PROPERTIES = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "app.authentication.secret",
            "app.authentication.issuer",
            "app.refresh-token.hmac-secret",
            "app.cors.allowed-origins",
            "app.frontend.url",
            "app.storage.endpoint",
            "app.storage.access-key",
            "app.storage.secret-key",
            "app.storage.bucket",
            "app.mail.from",
            "app.mail.from-name",
            "app.admin.email",
            "app.admin.password",
            "resend.api-key");

    // Hosts for which plain HTTP is acceptable, because the traffic never leaves the machine.
    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    @Override
    public int getOrder() {
        // Config data (application-prod.yaml, .env, profiles) is loaded by another listener on this
        // same event. Running last is what guarantees we read the fully resolved environment.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        if (env.matchesProfiles(PROD_PROFILE)) {
            validate(env);
        }
    }

    /**
     * Runs every production check and throws once, listing all failures together. Reporting them in
     * a batch matters: a deployer fixing a misconfigured server should not have to restart the
     * application once per mistake to discover the next one.
     */
    public void validate(Environment env) {
        List<String> errors = new ArrayList<>();

        requireTrue("app.cookies.secure", env, errors);
        rejectUnsafeDdlMode(env, errors);
        requireNonBlank("app.authentication.secret", env, errors);
        requireNonBlank("app.refresh-token.hmac-secret", env, errors);
        rejectSharedTokenSecrets(env, errors);
        requireMinimumLength("app.authentication.secret", MINIMUM_SECRET_LENGTH, env, errors);
        requireMinimumLength("app.refresh-token.hmac-secret", MINIMUM_SECRET_LENGTH, env, errors);
        requireNonBlank("app.authentication.issuer", env, errors);
        validateCorsOrigins(env, errors);
        requireNonBlank("app.storage.endpoint", env, errors);
        requireNonBlank("app.storage.access-key", env, errors);
        requireNonBlank("app.storage.secret-key", env, errors);
        requireNonBlank("app.storage.bucket", env, errors);
        requireNonBlank("app.storage.max-cv-size", env, errors);
        rejectDefaultStorageCredentials(env, errors);
        validateStorageEndpointScheme(env, errors);
        rejectLogOnlyMail(env, errors);
        validateFrontendUrl(env, errors);
        validateMailSender(env, errors);
        validateAdminBootstrap(env, errors);
        rejectPlaceholderValues(env, errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration:\n - " + String.join("\n - ", errors));
        }
    }

    private void requireTrue(String property, Environment env, List<String> errors) {
        if (!env.getProperty(property, Boolean.class, false)) {
            errors.add(property + " must be true in production");
        }
    }

    private void rejectUnsafeDdlMode(Environment env, List<String> errors) {
        String ddlMode = env.getProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .toLowerCase(Locale.ROOT);
        if (UNSAFE_DDL_MODES.contains(ddlMode)) {
            errors.add("spring.jpa.hibernate.ddl-auto must not be " + ddlMode + " in production");
        }
    }

    private void requireNonBlank(String property, Environment env, List<String> errors) {
        if (!StringUtils.hasText(env.getProperty(property))) {
            errors.add(property + " must be set in production");
        }
    }

    private void requireMinimumLength(String property, int minimumLength, Environment env, List<String> errors) {
        String value = env.getProperty(property);
        if (StringUtils.hasText(value) && value.length() < minimumLength) {
            errors.add(property + " must be at least " + minimumLength + " characters in production");
        }
    }

    private void rejectSharedTokenSecrets(Environment env, List<String> errors) {
        String jwtSecret = env.getProperty("app.authentication.secret");
        String refreshSecret = env.getProperty("app.refresh-token.hmac-secret");
        if (StringUtils.hasText(jwtSecret) && jwtSecret.equals(refreshSecret)) {
            errors.add("JWT and refresh-token HMAC secrets must be different in production");
        }
    }

    // Log-only mail prints verification links and initial passwords into the logs and never
    // delivers anything — a dev convenience that must not reach production.
    private void rejectLogOnlyMail(Environment env, List<String> errors) {
        if (env.getProperty("app.mail.log-only", Boolean.class, false)) {
            errors.add("app.mail.log-only must not be enabled in production");
        }
    }

    private void rejectDefaultStorageCredentials(Environment env, List<String> errors) {
        String accessKey = env.getProperty("app.storage.access-key");
        String secretKey = env.getProperty("app.storage.secret-key");
        if ("minioadmin".equals(accessKey) || "minioadmin".equals(secretKey)) {
            errors.add("MinIO storage credentials must not use default minioadmin values in production");
        }
    }

    /**
     * Resume files are personal data and the credentials to fetch them travel with every request,
     * so the object store must be reached over TLS. The one exception is a loopback address: a
     * MinIO running on the same host never puts those bytes on a network.
     */
    private void validateStorageEndpointScheme(Environment env, List<String> errors) {
        String endpoint = env.getProperty("app.storage.endpoint");
        if (!StringUtils.hasText(endpoint)) {
            return;
        }

        URI uri = parseUri(endpoint);
        if (uri == null || uri.getHost() == null) {
            errors.add("app.storage.endpoint must be a valid absolute URL: " + endpoint);
            return;
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"https".equals(scheme) && !"http".equals(scheme)) {
            errors.add("app.storage.endpoint must use HTTP or HTTPS: " + endpoint);
            return;
        }
        if ("https".equals(scheme) || ("http".equals(scheme) && isLoopback(uri.getHost()))) {
            return;
        }
        errors.add("app.storage.endpoint must use HTTPS in production unless it targets loopback: " + endpoint);
    }

    private void validateFrontendUrl(Environment env, List<String> errors) {
        String frontendUrl = env.getProperty("app.frontend.url");
        if (!StringUtils.hasText(frontendUrl)) {
            errors.add("app.frontend.url must be set in production");
            return;
        }

        if (frontendUrl.endsWith("/")) {
            errors.add("app.frontend.url must not end with a trailing slash: " + frontendUrl);
        }

        URI uri = parseUri(frontendUrl);
        if (uri == null || uri.getHost() == null) {
            errors.add("app.frontend.url must be a valid absolute URL: " + frontendUrl);
            return;
        }

        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            errors.add("app.frontend.url must not contain a query string or fragment: " + frontendUrl);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            errors.add("app.frontend.url must use HTTPS in production: " + frontendUrl);
        }
        if (isLoopback(uri.getHost())) {
            errors.add("app.frontend.url must not point at a local development host: " + frontendUrl);
        }
    }

    /**
     * Every outbound email is rejected by the provider if the sender is not on a verified domain,
     * and a wrong Resend key fails the same way, but only at send time, which is to say the first
     * time a real user registers. Both are checked at startup instead.
     */
    private void validateMailSender(Environment env, List<String> errors) {
        String from = env.getProperty("app.mail.from");
        if (!StringUtils.hasText(from)) {
            errors.add("app.mail.from must be set in production");
        } else if (!isValidEmail(from)) {
            errors.add("app.mail.from must be a valid email address: " + from);
        }

        requireNonBlank("app.mail.from-name", env, errors);

        String resendApiKey = env.getProperty("resend.api-key");
        if (!StringUtils.hasText(resendApiKey)) {
            errors.add("resend.api-key must be set in production");
        } else if (!resendApiKey.startsWith("re_")) {
            errors.add("resend.api-key does not look like a Resend key (expected a re_ prefix)");
        }
    }

    /**
     * The bootstrap admin is optional by design: once the account exists, {@code AdminInitializer}
     * skips it and the variables are meant to be removed. What is not acceptable is a half-set or
     * example-valued pair, which would either skip the bootstrap silently on a fresh database or
     * create the first administrator with a password that is published in this repository.
     */
    private void validateAdminBootstrap(Environment env, List<String> errors) {
        String adminEmail = env.getProperty("app.admin.email");
        String adminPassword = env.getProperty("app.admin.password");
        boolean hasEmail = StringUtils.hasText(adminEmail);
        boolean hasPassword = StringUtils.hasText(adminPassword);

        if (!hasEmail && !hasPassword) {
            return;
        }

        if (hasEmail != hasPassword) {
            errors.add("app.admin.email and app.admin.password must be set together, or neither");
        }
        if (hasEmail && !isValidEmail(adminEmail)) {
            errors.add("app.admin.email must be a valid email address: " + adminEmail);
        }
        if (hasPassword && adminPassword.length() < MINIMUM_ADMIN_PASSWORD_LENGTH) {
            errors.add("app.admin.password must be at least " + MINIMUM_ADMIN_PASSWORD_LENGTH + " characters in production");
        }
    }

    /**
     * Values are reported by property name only. The whole point of this check is that the value is
     * a template rather than a secret, but a typo could still make it a real one, and startup
     * failures are logged.
     */
    private void rejectPlaceholderValues(Environment env, List<String> errors) {
        for (String property : PLACEHOLDER_SCANNED_PROPERTIES) {
            String value = env.getProperty(property);
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String normalizedValue = value.toLowerCase(Locale.ROOT);
            PLACEHOLDER_MARKERS.stream()
                    .filter(normalizedValue::contains)
                    .findFirst()
                    .ifPresent(marker -> errors.add(
                            property + " still holds an example placeholder value (contains '" + marker + "')"));
        }
    }

    private void validateCorsOrigins(Environment env, List<String> errors) {
        String configuredOrigins = env.getProperty("app.cors.allowed-origins");
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
            if (origin.contains("*")) {
                errors.add("app.cors.allowed-origins must not include wildcard origins in production");
                continue;
            }

            URI uri = parseUri(origin);
            if (uri == null || uri.getHost() == null
                    || uri.getRawUserInfo() != null
                    || !uri.getRawPath().isEmpty()
                    || uri.getRawQuery() != null
                    || uri.getRawFragment() != null
                    || uri.getPort() > 65535
                    || uri.getRawAuthority().endsWith(":")) {
                errors.add("app.cors.allowed-origins must contain valid origins (scheme, host and optional port only): " + origin);
                continue;
            }

            if (isLoopback(uri.getHost())) {
                errors.add("app.cors.allowed-origins must not include local development origins in production");
            }
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                errors.add("app.cors.allowed-origins must use HTTPS origins in production: " + origin);
            }
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }

    private static boolean isLoopback(String host) {
        return LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));
    }

    private static URI parseUri(String value) {
        try {
            URI uri = new URI(value);
            return uri.isAbsolute() ? uri : null;
        } catch (URISyntaxException _) {
            return null;
        }
    }
}
