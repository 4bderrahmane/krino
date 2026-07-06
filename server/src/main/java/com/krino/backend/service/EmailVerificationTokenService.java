package com.krino.backend.service;

import com.krino.backend.entity.EmailVerificationToken;
import com.krino.backend.entity.User;
import com.krino.backend.repository.EmailVerificationTokenRepository;
import com.krino.backend.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TokenHasher tokenHasher;

    private static final int TOKEN_BYTES = 32;
    // Verification links grant no access by themselves, so they can live longer than the
    // 30-minute password-reset window.
    private static final int EXPIRY_HOURS = 24;

    public String issueToken(User user) {
        emailVerificationTokenRepository.markAllUsedForUser(user.getId());

        String rawToken = tokenHasher.generateUrlSafeToken(TOKEN_BYTES);
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash(tokenHasher.hmac(rawToken))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(EXPIRY_HOURS, ChronoUnit.HOURS))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(token);
        return rawToken;
    }

    public Optional<EmailVerificationToken> consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return emailVerificationTokenRepository.findValidByHashForUpdate(tokenHasher.hmac(rawToken), Instant.now())
                .map(token -> {
                    token.setUsed(true);
                    return token;
                });
    }

    public void deleteExpired() {
        emailVerificationTokenRepository.deleteExpired(Instant.now());
    }
}
