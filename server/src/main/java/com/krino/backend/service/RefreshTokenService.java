package com.krino.backend.service;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import com.krino.backend.entity.RefreshToken;
import com.krino.backend.entity.User;
import com.krino.backend.mapper.RefreshTokenMapper;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.security.TokenHasher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenMapper refreshTokenMapper;
    private final TokenHasher tokenHasher;
    private final AuthenticationProperties authenticationProperties;

    private static final int TOKEN_LENGTH = 64;

    public String generateAndSaveRefreshToken(User user, String deviceInfo, String ipAddress) {
        String token = tokenHasher.generateUrlSafeToken(TOKEN_LENGTH);

        byte[] tokenHash = tokenHasher.hmac(token);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(authenticationProperties.refreshTtl());

        RefreshToken refreshToken = refreshTokenMapper.toEntity(user, tokenHash, expiresAt, now, deviceInfo,
                ipAddress);

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public boolean validateToken(String token, byte[] storedHash) {
        return tokenHasher.matches(token, storedHash);
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

        return refreshTokenRepository.findValidTokenByHash(tokenHasher.hmac(token), Instant.now());
    }

    public Optional<RefreshToken> findValidRefreshTokenForUpdate(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return refreshTokenRepository.findValidTokenByHashForUpdate(tokenHasher.hmac(token), Instant.now());
    }

    /**
     * Looks a token up regardless of consumed/revoked/expired state, so callers can tell a
     * replayed (already-consumed) token apart from one that never existed at all.
     */
    public Optional<RefreshToken> findRefreshTokenAnyState(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        return refreshTokenRepository.findByTokenHash(tokenHasher.hmac(token));
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

    /**
     * Response to refresh-token reuse: revoke every session in the user's family. Runs in
     * its own transaction because the caller rejects the request by throwing right after —
     * a rollback of the surrounding transaction must not undo the revocation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCompromisedToken(Long userId) {
        revokeAllTokensForUser(userId);
    }
}
