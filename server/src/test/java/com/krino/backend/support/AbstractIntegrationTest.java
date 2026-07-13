package com.krino.backend.support;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for every test that boots the full application context. Provisions the production
 * stack: PostgreSQL, MinIO and Redis via Testcontainers, so the Flyway migrations, the
 * PostgreSQL-only constraints (partial indexes, CHECK clauses), real object storage, and the
 * Redis backed cache/rate limiter are exercised instead of an in-memory stand-in.
 *
 * <p>The containers are static singletons: started once per test JVM, shared by every Spring
 * context the suite creates, and removed by Testcontainers' reaper when the JVM exits.
 * Contexts may boot several times against the same database; that is safe because Flyway
 * re-runs are no-ops and every test class wipes the tables it uses.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine");

    private static final MinIOContainer MINIO =
            new MinIOContainer(DockerImageName
            .parse("quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z")
                    .asCompatibleSubstituteFor("minio/minio"));

    private static final RedisContainer REDIS =
            new RedisContainer("redis:8-alpine");

    static {
        Startables.deepStart(POSTGRES, MINIO, REDIS).join();
    }

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    // Postgres isolation comes from each test class wiping its tables; Redis (cache entries,
    // rate-limit buckets) is the state that would otherwise survive from test to test, so
    // flush it before every test.
    @BeforeEach
    void flushRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.storage.endpoint", MINIO::getS3URL);
        registry.add("app.storage.access-key", MINIO::getUserName);
        registry.add("app.storage.secret-key", MINIO::getPassword);
        registry.add("spring.data.redis.host", REDIS::getRedisHost);
        registry.add("spring.data.redis.port", REDIS::getRedisPort);
    }
}
