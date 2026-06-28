package com.krino.backend.security;

import com.krino.backend.configuration.RateLimitProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class Bucket4jRateLimiterTest {

    private static RateLimiter limiterWithCapacity(long capacity) {
        RateLimitProperties properties = new RateLimitProperties(
                true, capacity, capacity, Duration.ofHours(1), Duration.ofHours(2), 1_000);
        return new Bucket4jRateLimiter(properties);
    }

    @Test
    void allowsUpToCapacityThenThrottles() {
        RateLimiter limiter = limiterWithCapacity(2);

        assertThat(limiter.tryConsume("ip:1.1.1.1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip:1.1.1.1").allowed()).isTrue();

        RateLimiter.RateLimitResult throttled = limiter.tryConsume("ip:1.1.1.1");
        assertThat(throttled.allowed()).isFalse();
        assertThat(throttled.remaining()).isZero();
        assertThat(throttled.retryAfter()).isPositive();
    }

    @Test
    void keysAreLimitedIndependently() {
        RateLimiter limiter = limiterWithCapacity(1);

        assertThat(limiter.tryConsume("ip:1.1.1.1").allowed()).isTrue();
        assertThat(limiter.tryConsume("ip:1.1.1.1").allowed()).isFalse();
        // A different caller gets its own fresh bucket.
        assertThat(limiter.tryConsume("ip:2.2.2.2").allowed()).isTrue();
    }
}
