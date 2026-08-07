package com.krino.backend.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.ratelimit")
public record RateLimitProperties(
        boolean enabled,
        long capacity,
        Duration refillPeriod,
        Duration idleExpiry,
        Tier publicBrowse) {

    /**
     * One bucket family's budget. The authentication endpoints and the anonymous browse
     * endpoints are throttled independently: a login budget tight enough to blunt credential
     * stuffing (a handful per minute) would make normal catalogue browsing fail, so they
     * cannot share a tier.
     */
    public record Tier(long capacity, Duration refillPeriod) {

        public Tier {
            if (capacity <= 0)
                throw new IllegalArgumentException("capacity should be > 0");
            if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative())
                throw new IllegalArgumentException("refillPeriod should be > 0");
        }
    }

    public RateLimitProperties {
        if (enabled) {
            if (capacity <= 0)
                throw new IllegalArgumentException("capacity should be > 0");
            if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative())
                throw new IllegalArgumentException("refillPeriod should be > 0");
            if (idleExpiry == null || idleExpiry.isNegative())
                throw new IllegalArgumentException("idleExpiry should be >= 0");
            if (publicBrowse == null)
                throw new IllegalArgumentException("public-browse tier is required when rate limiting is enabled");
        }
    }
}
