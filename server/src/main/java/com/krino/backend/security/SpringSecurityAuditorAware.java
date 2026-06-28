package com.krino.backend.security;

import com.krino.backend.utility.SecurityUtilities;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Supplies the current auditor for Spring Data JPA {@code @CreatedBy}/{@code @LastModifiedBy}.
 * The auditor is the authenticated user's email, falling back to {@code "system"}
 * for unauthenticated or background writes (seeding, scheduled jobs) so the
 * NOT NULL "who" columns are always populated.
 */
@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

    public static final String SYSTEM = "system";

    @Override
    public @NonNull Optional<String> getCurrentAuditor() {
        return Optional.of(SecurityUtilities.getCurrentUsername().orElse(SYSTEM));
    }
}
