package com.krino.backend.service;

import com.krino.backend.entity.PasswordResetToken;
import com.krino.backend.entity.User;
import com.krino.backend.exception.TokenException;
import com.krino.backend.repository.PasswordResetTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureRandom secureRandom;

    @Value("${app.refresh-token.hmac-secret:${jwt.secret}}")
    private String hmacSecret;

    private SecretKeySpec hmacKey;

    private static final int TOKEN_BYTES = 32;
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int EXPIRY_MINUTES = 30;

    @PostConstruct
    void init() {
        if (hmacSecret == null || hmacSecret.length() < 32)
            throw new TokenException("Invalid password-reset HMAC secret: it must be at least 32 characters.");
        this.hmacKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    public String issueToken(User user) {
        passwordResetTokenRepository.markAllUsedForUser(user.getId());

        String rawToken = generateRawToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(hmac(rawToken))
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
        return passwordResetTokenRepository.findValidByHashForUpdate(hmac(rawToken), Instant.now())
                .map(token -> {
                    token.setUsed(true);
                    return token;
                });
    }

    public void deleteExpired() {
        passwordResetTokenRepository.deleteExpired(Instant.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] hmac(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(hmacKey);
            return mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
