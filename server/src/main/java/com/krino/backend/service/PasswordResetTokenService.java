package com.krino.backend.service;

import com.krino.backend.entity.PasswordResetToken;
import com.krino.backend.entity.User;
import com.krino.backend.repository.PasswordResetTokenRepository;
import com.krino.backend.security.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TokenHasher tokenHasher;

    private static final int TOKEN_BYTES = 32;
    private static final int EXPIRY_MINUTES = 30;

    public String issueToken(User user) {
        passwordResetTokenRepository.markAllUsedForUser(user.getId());

        String rawToken = tokenHasher.generateUrlSafeToken(TOKEN_BYTES);
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(tokenHasher.hmac(rawToken))
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES))
                .used(false)
                .build();
        passwordResetTokenRepository.save(token);
        return rawToken;
    }

    public Optional<PasswordResetToken> consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return passwordResetTokenRepository.findValidByHashForUpdate(tokenHasher.hmac(rawToken), Instant.now())
                .map(token -> {
                    token.setUsed(true);
                    return token;
                });
    }

    public void deleteExpired() {
        passwordResetTokenRepository.deleteExpired(Instant.now());
    }
}
