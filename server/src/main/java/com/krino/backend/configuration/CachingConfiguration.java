package com.krino.backend.configuration;

import com.krino.backend.dto.common.PageResponse;
import com.krino.backend.dto.department.DepartmentResponseDTO;
import com.krino.backend.dto.job.JobResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
@Slf4j
public class CachingConfiguration {

    public static final String JOB_LISTINGS_CACHE = "jobListings";
    public static final String JOBS_CACHE = "jobs";
    public static final String DEPARTMENTS_CACHE = "departments";
    private static final String KEY_PREFIX = "krino::";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisCacheWriter cacheWriter = RedisCacheWriter.create(factory, RedisCacheWriter.RedisCacheWriterConfigurer::immediateWrites);

        // transactionAware: cache puts/evicts inside a transaction are deferred to commit,
        // so a rolled-back mutation can never leave its (phantom) state in the cache.
        //
        // disableCreateOnMissingCache: every cache is declared below with a serializer pinned to
        // its value type, so an unknown cache name is a bug, not a cache to invent with untyped
        // defaults. Spring fails the call instead of quietly falling back.
        return RedisCacheManager.builder(cacheWriter)
                .transactionAware()
                .disableCreateOnMissingCache()
                .withCacheConfiguration(JOB_LISTINGS_CACHE,
                        typedCache(objectMapper, Duration.ofMinutes(2), pageOf(objectMapper, JobResponseDTO.class)))
                .withCacheConfiguration(JOBS_CACHE,
                        typedCache(objectMapper, Duration.ofMinutes(10), objectMapper.constructType(JobResponseDTO.class)))
                .withCacheConfiguration(DEPARTMENTS_CACHE,
                        typedCache(objectMapper, Duration.ofHours(1), listOf(objectMapper, DepartmentResponseDTO.class)))
                .build();
    }

    private RedisCacheConfiguration typedCache(ObjectMapper objectMapper, Duration ttl, JavaType valueType) {
        var valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, valueType);
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer))
                .entryTtl(ttl)
                .prefixCacheNameWith(KEY_PREFIX)
                // Nulls are never stored, so the cache never has to represent one:
                // every @Cacheable method here either returns a value or throws.
                .disableCachingNullValues();
    }

    private static JavaType pageOf(ObjectMapper objectMapper, Class<?> elementType) {
        return objectMapper.getTypeFactory().constructParametricType(PageResponse.class, elementType);
    }

    private static JavaType listOf(ObjectMapper objectMapper, Class<?> elementType) {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
    }
}
