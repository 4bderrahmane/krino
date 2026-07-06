package com.krino.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for every test that boots the full application context. Provisions the production
 * stack — PostgreSQL and MinIO — via Testcontainers, so the Flyway migrations, the
 * PostgreSQL-only constraints (partial indexes, CHECK clauses) and real object storage are
 * exercised instead of an in-memory stand-in.
 *
 * <p>The containers are static singletons: started once per test JVM, shared by every Spring
 * context the suite creates, and removed by Testcontainers' reaper when the JVM exits.
 * Contexts may boot several times against the same database; that is safe because Flyway
 * re-runs are no-ops and every test class wipes the tables it uses.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    // postgres:16 instead of compose's postgres:18-alpine: it is the image already present
    // locally, and nothing under test depends on the major version. MinIO matches compose's
    // image; the quay.io mirror must be declared compatible with the Docker Hub name the
    // Testcontainers module expects.
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
    private static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("quay.io/minio/minio:latest").asCompatibleSubstituteFor("minio/minio"));

    static {
        Startables.deepStart(POSTGRES, MINIO).join();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.storage.endpoint", MINIO::getS3URL);
        registry.add("app.storage.access-key", MINIO::getUserName);
        registry.add("app.storage.secret-key", MINIO::getPassword);
    }
}
