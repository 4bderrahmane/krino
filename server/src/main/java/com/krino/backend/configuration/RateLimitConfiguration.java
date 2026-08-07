package com.krino.backend.configuration;

import com.krino.backend.configuration.properties.RateLimitProperties;
import com.krino.backend.exception.ExceptionProblemDetailFactory;
import com.krino.backend.security.Bucket4jRateLimiter;
import com.krino.backend.security.ClientKeyResolver;
import com.krino.backend.security.RateLimitFilter;
import com.krino.backend.security.RateLimiter;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = "app.ratelimit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitConfiguration {

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient(DataRedisProperties redis) {
        RedisURI.Builder uri = RedisURI.builder().withHost(redis.getHost()).withPort(redis.getPort());
        String password = redis.getPassword();
        if (password != null && !password.isEmpty()) {
            uri.withPassword(password.toCharArray());
        }
        return RedisClient.create(uri.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient rateLimitRedisClient) {
        return rateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    public ProxyManager<String> rateLimitProxyManager(RateLimitProperties properties,
                                                      StatefulRedisConnection<String, byte[]> rateLimitRedisConnection) {
        return Bucket4jLettuce.casBasedBuilder(rateLimitRedisConnection)
                .expirationAfterWrite(ExpirationAfterWriteStrategy
                        .basedOnTimeForRefillingBucketUpToMax(properties.idleExpiry()))
                .build();
    }

    /**
     * Guards credential-bearing endpoints: a tight budget, because each request is an attempt.
     */
    @Bean
    public RateLimiter authRateLimiter(RateLimitProperties properties, ProxyManager<String> rateLimitProxyManager) {
        return new Bucket4jRateLimiter("auth", properties.capacity(), properties.refillPeriod(), rateLimitProxyManager);
    }

    /**
     * Guards the anonymous catalogue endpoints. Browsing costs several requests per page view,
     * so this budget is far looser than the authentication one; it exists to cap scraping, not
     * to make the site feel broken.
     */
    @Bean
    public RateLimiter publicBrowseRateLimiter(RateLimitProperties properties,
                                               ProxyManager<String> rateLimitProxyManager) {
        return new Bucket4jRateLimiter("public", properties.publicBrowse().capacity(),
                properties.publicBrowse().refillPeriod(), rateLimitProxyManager);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(@Qualifier("authRateLimiter") RateLimiter authRateLimiter,
                                           @Qualifier("publicBrowseRateLimiter") RateLimiter publicBrowseRateLimiter,
                                           ClientKeyResolver clientKeyResolver,
                                           ExceptionProblemDetailFactory problemDetailFactory,
                                           ObjectMapper objectMapper) {
        return new RateLimitFilter(authRateLimiter, publicBrowseRateLimiter, clientKeyResolver, problemDetailFactory, objectMapper);
    }
}
