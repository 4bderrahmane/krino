package com.krino.backend.security;

import com.krino.backend.configuration.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Token-bucket rate limiter backed by Bucket4j, with one bucket per key held in Redis, so the
 * limit holds across application instances and restarts. The proxy manager's expiration
 * strategy keeps a bucket alive until it has fully refilled plus an idle margin (see
 * {@code RateLimitConfiguration}), so a throttled caller's bucket is never expired (and reset)
 * while it is still being limited; idle keys expire on their own, so memory stays bounded.
 */
public class Bucket4jRateLimiter implements RateLimiter {

    /** Namespaces bucket keys in Redis, away from the "krino::" cache entries. */
    private static final String KEY_PREFIX = "ratelimit:";

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfiguration;
    private final long limit;

    public Bucket4jRateLimiter(RateLimitProperties properties, ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
        this.limit = properties.capacity();

        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(properties.capacity())
                .refillGreedy(properties.refillTokens(), properties.refillPeriod())
                .build();
        BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
        this.bucketConfiguration = () -> configuration;
    }

    @Override
    public RateLimitResult tryConsume(String key) {
        BucketProxy bucket = proxyManager.builder().build(KEY_PREFIX + key, bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        return new RateLimitResult(
                probe.isConsumed(),
                limit,
                probe.getRemainingTokens(),
                Duration.ofNanos(probe.getNanosToWaitForRefill())
        );
    }
}
