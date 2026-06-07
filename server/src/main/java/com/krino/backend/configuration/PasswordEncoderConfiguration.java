package com.krino.backend.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfiguration
{

    /**
     * Using BCrypt rather than Argon2id.
     * <p>
     * Argon2id is memory hard by design: each hash allocates a fair amount
     * of RAM (OWASP baseline ~19 MiB). That cost is PER hashing operation, and
     * every concurrent sign-up or login runs one. Under a burst of simultaneous
     * auth requests on a constrained host, that memory pressure becomes a
     * practical DoS vector and safely deploying Argon2id therefore also
     * requires rate-limiting in front of the auth endpoints.
     * </p>
     *
     * <p>
     * BCrypt's memory footprint is small and fixed (~4 KiB), so it stays
     * predictable under concurrency. At work factor 12 it remains an
     * OWASP accepted choice; the extra GPU/ASIC-cracking resistance Argon2id
     * provides is not worth the operational cost for this project's threat model.
     * </p>
     *
     * <p>
     * But still worth mentioning that Argon2id is preferable when having enough resources.
     * </p>
     */

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder(12);
    }
}
