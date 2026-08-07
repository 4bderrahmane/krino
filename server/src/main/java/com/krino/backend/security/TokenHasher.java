package com.krino.backend.security;

import com.krino.backend.exception.TokenException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Shared primitives for opaque, single-use auth tokens (refresh, password reset, email
 * verification): CSPRNG raw-token generation and keyed HMAC-SHA256 hashing, so only the
 * hash is ever persisted and a database leak never exposes a usable token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenHasher {
    public static final int HMAC_SHA256_LENGTH = 32;
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SecureRandom secureRandom;

    @Value("${app.refresh-token.hmac-secret:${app.authentication.secret}}")
    private String hmacSecret;

    private SecretKeySpec hmacKey;

    @PostConstruct
    void init() {
        if (hmacSecret == null || hmacSecret.length() < 32) {
            throw new TokenException("Invalid token HMAC secret: it must be at least 32 characters.");
        }
        this.hmacKey = new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    /** A URL-safe base64 token from {@code numBytes} of CSPRNG output, without padding. */
    public String generateUrlSafeToken(int numBytes) {
        byte[] randomBytes = new byte[numBytes];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public byte[] hmac(String token) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(hmacKey);
            return mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    /** Constant-time comparison of a raw token against a stored HMAC hash. */
    public boolean matches(String token, byte[] storedHash) {
        if (token == null || storedHash == null || storedHash.length != HMAC_SHA256_LENGTH) {
            return false;
        }
        return MessageDigest.isEqual(storedHash, hmac(token));
    }
}
