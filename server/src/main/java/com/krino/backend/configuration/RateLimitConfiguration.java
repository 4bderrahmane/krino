package com.krino.backend.configuration;

import com.krino.backend.exception.ExceptionProblemDetailFactory;
import com.krino.backend.security.Bucket4jRateLimiter;
import com.krino.backend.security.ClientKeyResolver;
import com.krino.backend.security.RateLimitFilter;
import com.krino.backend.security.RateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires rate limiting only when {@code app.ratelimit.enabled} is true (the default). When
 * disabled — e.g. in tests — none of these beans exist, so {@link SecurityConfiguration}
 * simply skips the filter and no Bucket4j machinery is built.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "app.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfiguration {

    @Bean
    public RateLimiter rateLimiter(RateLimitProperties properties) {
        return new Bucket4jRateLimiter(properties);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimiter rateLimiter, ClientKeyResolver clientKeyResolver,
                                           ExceptionProblemDetailFactory problemDetailFactory,
                                           ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimiter, clientKeyResolver, problemDetailFactory, objectMapper);
    }
}
