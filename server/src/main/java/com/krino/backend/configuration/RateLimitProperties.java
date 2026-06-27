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
        Duration idleExpiry,
        long maxKeys) {

    public RateLimitProperties {
        if (enabled) {
            if (capacity <= 0) throw new IllegalArgumentException("capacity should be > 0");
            if (refillTokens <= 0) throw new IllegalArgumentException("refill tokens should be > 0");

            long periodsToFill = (long) Math.ceil((double) capacity / refillTokens);
            Duration fullRefill = refillPeriod.multipliedBy(periodsToFill);

            if (idleExpiry.compareTo(fullRefill) < 0)
                throw new IllegalArgumentException("idleExpiry " + idleExpiry + " < full refill " + fullRefill + " — " +
                        "a throttled client's bucket would evict and reset to full");
        }
    }
}
