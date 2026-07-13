package com.krino.backend.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class CachingConfiguration {

    public static final String JOB_LISTINGS_CACHE = "jobListings";
    public static final String JOBS_CACHE = "jobs";
    public static final String DEPARTMENTS_CACHE = "departments";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        var valueSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .enableSpringCacheNullValueSupport()
                .build();
        var defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("krino::")
                .disableCachingNullValues();

        // immediateWrites: Spring Data Redis 4 defers puts/evicts to a background pipeline by
        // default, so a read racing a mutation could still see (and keep serving) the evicted
        // entry. Synchronous writes make "mutation returned, cache is consistent" actually hold.
        var cacheWriter = RedisCacheWriter.create(factory, config -> config.immediateWrites());

        // transactionAware: cache puts/evicts inside a transaction are deferred to commit,
        // so a rolled-back mutation can never leave its (phantom) state in the cache.
        return RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(defaults)
                .transactionAware()
                .withCacheConfiguration(JOB_LISTINGS_CACHE, defaults.entryTtl(Duration.ofMinutes(2)))
                .withCacheConfiguration(JOBS_CACHE, defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(DEPARTMENTS_CACHE, defaults.entryTtl(Duration.ofHours(1)))
                .build();
    }
}
