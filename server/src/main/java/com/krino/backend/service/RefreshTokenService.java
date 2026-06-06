package com.krino.backend.service;

import com.krino.backend.entity.RefreshToken;
import com.krino.backend.entity.User;
import com.krino.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService
{
    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int TOKEN_LENGTH = 64;
    private static final int SALT_LENGTH = 16;
    private static final int EXPIRY_DAYS = 30;

    private String generateRandomToken()
    {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    //This method returns raw bytes: [salt (16)] + [sha256(tokenWithSalt) (32)] = 48 bytes
    private byte[] hashTokenWithSalt(String token)
    {
        try
        {
            byte[] salt = new byte[SALT_LENGTH];
            secureRandom.nextBytes(salt);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(token.getBytes());

            byte[] combined = new byte[salt.length + hash.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hash, 0, combined, salt.length, hash.length);

            return combined;

        } catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public String generateAndSaveRefreshToken(User user, String deviceInfo, String ipAddress)
    {
        String token = generateRandomToken();

        byte[] hashedToken = hashTokenWithSalt(token);

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(EXPIRY_DAYS);
        LocalDateTime now = LocalDateTime.now();

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashedToken);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setCreatedAt(now);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setIsRevoked(false);
        refreshToken.setLastUsedAt(now);

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    public boolean validateToken(String token, byte[] storedHash)
    {
        try
        {
            if (storedHash == null || storedHash.length < SALT_LENGTH + 32)
            {
                return false;
            }

            byte[] salt = new byte[SALT_LENGTH];
            byte[] originalHash = new byte[storedHash.length - SALT_LENGTH];
            System.arraycopy(storedHash, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(storedHash, SALT_LENGTH, originalHash, 0, storedHash.length - SALT_LENGTH);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] testHash = digest.digest(token.getBytes());

            return MessageDigest.isEqual(originalHash, testHash);

        } catch (Exception e)
        {
            return false;
        }
    }

    public boolean validateRefreshToken(String token)
    {
        LocalDateTime now = LocalDateTime.now();

        List<RefreshToken> validTokens = refreshTokenRepository.findAllValidTokens(now);

        for (RefreshToken refreshToken : validTokens)
        {
            if (validateToken(token, refreshToken.getTokenHash()))
            {
                return true;
            }
        }

        return false;
    }

    public Optional<User> getUserFromRefreshToken(String token)
    {
        LocalDateTime now = LocalDateTime.now();

        List<RefreshToken> validTokens = refreshTokenRepository.findAllValidTokens(now);

        for (RefreshToken refreshToken : validTokens)
        {
            if (validateToken(token, refreshToken.getTokenHash()))
            {
                return Optional.of(refreshToken.getUser());
            }
        }

        return Optional.empty();
    }

    public void updateLastUsed(String token)
    {
        LocalDateTime now = LocalDateTime.now();


        List<RefreshToken> validTokens = refreshTokenRepository.findAllValidTokens(now);

        for (RefreshToken refreshToken : validTokens)
        {
            if (validateToken(token, refreshToken.getTokenHash()))
            {
                refreshToken.setLastUsedAt(LocalDateTime.now());
                refreshTokenRepository.save(refreshToken);
                break;
            }
        }
    }

    public void revokeToken(RefreshToken token)
    {
        token.setIsRevoked(true);
        token.setLastUsedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
    }

    public void revokeAllTokensForUser(Long userId)
    {
        refreshTokenRepository.revokeAllUserTokens(userId);
    }

    public Optional<RefreshToken> findValidRefreshToken(String token)
    {
        LocalDateTime now = LocalDateTime.now();

        List<RefreshToken> validTokens = refreshTokenRepository.findAllValidTokens(now);

        for (RefreshToken refreshToken : validTokens)
        {
            if (validateToken(token, refreshToken.getTokenHash()))
            {
                return Optional.of(refreshToken);
            }
        }

        return Optional.empty();
    }

    public void cleanupExpiredAndRevokedTokens()
    {
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
    }

    public List<RefreshToken> findActiveTokensByUser(Long userId)
    {
        return refreshTokenRepository.findActiveTokensByUser(userId, LocalDateTime.now());
    }

    public long countActiveTokensByUser(Long userId)
    {
        return refreshTokenRepository.countActiveTokensByUser(userId, LocalDateTime.now());
    }

    public void handleCompromisedToken(Long userId)
    {
        revokeAllTokensForUser(userId);
    }
}
