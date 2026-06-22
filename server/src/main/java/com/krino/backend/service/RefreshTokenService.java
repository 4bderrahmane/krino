package com.krino.backend.service;

import com.krino.backend.entity.RefreshToken;
import com.krino.backend.entity.User;
import com.krino.backend.exception.TokenException;
import com.krino.backend.mapper.RefreshTokenMapper;
import com.krino.backend.repository.RefreshTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.refresh-token.hmac-secret:${jwt.secret}}")
    private String hmacSecret;

    private SecretKeySpec hmacKey;

    private static final int TOKEN_LENGTH = 64;
    private static final int HMAC_SHA256_LENGTH = 32;
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int EXPIRY_DAYS = 30;

    @PostConstruct
    void init() {
        if (hmacSecret == null || hmacSecret.length() < 32) {
            throw new TokenException("Invalid refresh-token HMAC secret: it must be at least 32 characters.");
        }

        this.hmacKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    private String generateRandomToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private byte[] hmacToken(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(hmacKey);
            return mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    public String generateAndSaveRefreshToken(User user, String deviceInfo, String ipAddress) {
        String token = generateRandomToken();

        byte[] tokenHash = hmacToken(token);

        Instant expiresAt = Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS);
        Instant now = Instant.now();

        RefreshToken refreshToken = refreshTokenMapper.toEntity(user, tokenHash, expiresAt, now, deviceInfo,
                ipAddress);

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public boolean validateToken(String token, byte[] storedHash) {
        if (token == null || storedHash == null || storedHash.length != HMAC_SHA256_LENGTH) {
            return false;
        }

        return MessageDigest.isEqual(storedHash, hmacToken(token));
    }

    public boolean validateRefreshToken(String token) {
        return findValidRefreshToken(token).isPresent();
    }

    public Optional<User> getUserFromRefreshToken(String token) {
        return findValidRefreshToken(token).map(RefreshToken::getUser);
    }

    public void revokeToken(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public void consumeToken(RefreshToken token) {
        token.setConsumed(true);
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    public void revokeAllTokensForUser(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    public Optional<RefreshToken> findValidRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return refreshTokenRepository.findValidTokenByHash(hmacToken(token), Instant.now());
    }

    public Optional<RefreshToken> findValidRefreshTokenForUpdate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return refreshTokenRepository.findValidTokenByHashForUpdate(hmacToken(token), Instant.now());
    }

    public void cleanupExpiredAndRevokedTokens() {
        refreshTokenRepository.deleteExpiredAndRevokedTokens(Instant.now());
    }

    public List<RefreshToken> findActiveTokensByUser(Long userId) {
        return refreshTokenRepository.findActiveTokensByUser(userId, Instant.now());
    }

    public long countActiveTokensByUser(Long userId) {
        return refreshTokenRepository.countActiveTokensByUser(userId, Instant.now());
    }

    public void handleCompromisedToken(Long userId) {
        revokeAllTokensForUser(userId);
    }
}
