package com.krino.backend.configuration;

import com.krino.backend.security.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate}
 * are filled automatically. The "who" columns ({@code @CreatedBy}/{@code @LastModifiedBy})
 * are supplied by {@link SpringSecurityAuditorAware}, resolved here by bean name.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class JpaAuditingConfiguration {
}
