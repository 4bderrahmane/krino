package com.krino.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;

import java.time.Duration;
import java.util.function.Supplier;

public class Bucket4jRateLimiter implements RateLimiter {

    private static final String KEY_PREFIX = "ratelimit:";

    private final ProxyManager<String> proxyManager;
    private final Supplier<BucketConfiguration> bucketConfiguration;
    private final String keyNamespace;
    private final long limit;

    public Bucket4jRateLimiter(String namespace, long capacity, Duration refillPeriod, ProxyManager<String> proxyManager) {
        this.proxyManager = proxyManager;
        this.keyNamespace = namespace + ":";
        this.limit = capacity;

        // Refill exactly `capacity` tokens per refill-period, so a drained bucket takes the
        // whole period to recover and the sustained rate can never exceed the burst capacity.
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, refillPeriod)
                .build();
        BucketConfiguration configuration = BucketConfiguration.builder().addLimit(bandwidth).build();
        this.bucketConfiguration = () -> configuration;
    }

    @Override
    public RateLimitResult tryConsume(String key) {
        BucketProxy bucket = proxyManager.builder().build(KEY_PREFIX + keyNamespace + key, bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        return new RateLimitResult(
                probe.isConsumed(),
                limit,
                probe.getRemainingTokens(),
                Duration.ofNanos(probe.getNanosToWaitForRefill())
        );
    }
}
