package com.krino.backend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.ratelimit")
public record RateLimitProperties(
        boolean enabled,
        long capacity,
        long refillTokens,
        Duration refillPeriod,
        Duration idleExpiry) {

    public RateLimitProperties {
        if (enabled) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity should be > 0");
            if (refillTokens <= 0) throw new IllegalArgumentException("refill tokens should be > 0");
            if (idleExpiry == null || idleExpiry.isNegative()) throw new IllegalArgumentException("idleExpiry should be >= 0");
        }
    }
}
