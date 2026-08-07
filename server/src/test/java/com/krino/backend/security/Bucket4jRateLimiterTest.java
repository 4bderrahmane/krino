package com.krino.backend.security;

import com.krino.backend.configuration.properties.RateLimitProperties;
import com.redis.testcontainers.RedisContainer;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against a throwaway Redis container, wired exactly like RateLimitConfiguration wires
 * the production limiter. Keys are randomized per test because the Redis instance (and thus
 * bucket state) is shared across the class.
 */
class Bucket4jRateLimiterTest {

    private static final RedisContainer REDIS = new RedisContainer("redis:8-alpine");

    private static RedisClient redisClient;
    private static StatefulRedisConnection<String, byte[]> connection;

    @BeforeAll
    static void startRedis() {
        REDIS.start();
        redisClient = RedisClient.create("redis://" + REDIS.getRedisHost() + ":" + REDIS.getRedisPort());
        connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @AfterAll
    static void stopRedis() {
        connection.close();
        redisClient.shutdown();
        REDIS.stop();
    }

    private static RateLimiter limiterWithCapacity(long capacity) {
        return limiterWithCapacity("auth", capacity);
    }

    private static RateLimiter limiterWithCapacity(String namespace, long capacity) {
        var proxyManager = Bucket4jLettuce.casBasedBuilder(connection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(2)))
                .build();
        return new Bucket4jRateLimiter(namespace, capacity, Duration.ofHours(1), proxyManager);
    }

    private static String randomKey() {
        return "ip:" + UUID.randomUUID();
    }

    @Test
    void allowsUpToCapacityThenThrottles() {
        RateLimiter limiter = limiterWithCapacity(2);
        String key = randomKey();

        assertThat(limiter.tryConsume(key).allowed()).isTrue();
        assertThat(limiter.tryConsume(key).allowed()).isTrue();

        RateLimiter.RateLimitResult throttled = limiter.tryConsume(key);
        assertThat(throttled.allowed()).isFalse();
        assertThat(throttled.remaining()).isZero();
        assertThat(throttled.retryAfter()).isPositive();
    }

    @Test
    void tiersDoNotShareABucketForTheSameKey() {
        // A bucket's capacity is fixed when Redis first creates it, so if the two tiers named
        // the same bucket, the public tier would inherit the exhausted auth bucket and browsing
        // would be throttled by the login budget.
        RateLimiter authLimiter = limiterWithCapacity("auth", 1);
        RateLimiter publicLimiter = limiterWithCapacity("public", 3);
        String sharedKey = randomKey();

        assertThat(authLimiter.tryConsume(sharedKey).allowed()).isTrue();
        assertThat(authLimiter.tryConsume(sharedKey).allowed()).isFalse();

        RateLimiter.RateLimitResult publicResult = publicLimiter.tryConsume(sharedKey);
        assertThat(publicResult.allowed()).isTrue();
        assertThat(publicResult.limit()).isEqualTo(3);
    }

    @Test
    void keysAreLimitedIndependently() {
        RateLimiter limiter = limiterWithCapacity(1);
        String throttledKey = randomKey();

        assertThat(limiter.tryConsume(throttledKey).allowed()).isTrue();
        assertThat(limiter.tryConsume(throttledKey).allowed()).isFalse();
        // A different caller gets its own fresh bucket.
        assertThat(limiter.tryConsume(randomKey()).allowed()).isTrue();
    }
}
