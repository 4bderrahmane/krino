package com.krino.backend.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.krino.backend.configuration.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.BucketProxy;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Token-bucket rate limiter backed by Bucket4j, with one bucket per key held in a Caffeine
 * cache. The cache caps the number of live keys ({@code maxKeys}) so an attacker rotating
 * source IPs cannot exhaust the heap; Bucket4j's {@link CaffeineProxyManager} additionally
 * keeps a bucket alive until it has fully refilled, so a throttled caller's bucket is never
 * evicted (and reset) while it is still being limited.
 */
public class Bucket4jRateLimiter implements RateLimiter {

    private final CaffeineProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfiguration;
    private final long limit;

    public Bucket4jRateLimiter(RateLimitProperties properties) {
        this.limit = properties.capacity();

        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(properties.capacity())
                .refillGreedy(properties.refillTokens(), properties.refillPeriod())
                .build();
        BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
        this.bucketConfiguration = () -> configuration;

        // Only maximumSize here: CaffeineProxyManager installs its own (variable) expiry,
        // and Caffeine forbids combining that with expireAfterAccess/Write.
        Caffeine<Object, Object> cache = Caffeine.newBuilder().maximumSize(properties.maxKeys());
        this.proxyManager = new CaffeineProxyManager<>(cache, properties.idleExpiry());
    }

    @Override
    public RateLimitResult tryConsume(String key) {
        BucketProxy bucket = proxyManager.builder().build(key, bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        return new RateLimitResult(
                probe.isConsumed(),
                limit,
                probe.getRemainingTokens(),
                Duration.ofNanos(probe.getNanosToWaitForRefill())
        );
    }
}
