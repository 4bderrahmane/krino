package com.krino.backend.service;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.exception.TokenException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String EXPECTED_ALGORITHM = "HS256";

    private final AuthenticationProperties properties;

    private SecretKey signingKey;
    private JwtParser jwtParser;

    @PostConstruct
    protected void init() {
        if (properties.secret().length() < 32)
            throw new TokenException("Invalid JWT secret: it must be at least 32 characters for HS256.");

        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build();
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTtl());

        return Jwts.builder()
                .subject(userDetails.getPublicId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .issuer(properties.issuer())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public VerifiedAccessToken parseAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Access token must not be blank");
        }

        try {
            Jws<Claims> parsedToken = jwtParser.parseSignedClaims(token);
            validateHeader(parsedToken);

            Claims claims = parsedToken.getPayload();
            UUID userPublicId = validateAccessTokenClaims(claims);
            return new VerifiedAccessToken(userPublicId, claims.getExpiration().toInstant());
        } catch (JwtException ex) {
            log.debug("Access-token validation failed: {}", ex.getMessage());
            throw new BadCredentialsException("Invalid access token", ex);
        }
    }

    public long getAccessTokenExpiryInSeconds() {
        return properties.accessTtl().toSeconds();
    }

    private void validateHeader(Jws<Claims> parsedToken) {
        String algorithm = parsedToken.getHeader().getAlgorithm();
        if (!EXPECTED_ALGORITHM.equals(algorithm)) {
            throw new UnsupportedJwtException("Unsupported JWT signing algorithm: " + algorithm);
        }
    }

    private UUID validateAccessTokenClaims(Claims claims) {
        UUID userPublicId = requireValidSubject(claims);
        requirePresent(claims.getIssuedAt(), "JWT issued-at claim is required");
        requirePresent(claims.getExpiration(), "JWT expiration claim is required");
        requireAccessTokenType(claims.get(TOKEN_TYPE_CLAIM, String.class));
        return userPublicId;
    }

    private UUID requireValidSubject(Claims claims) {
        String subject = claims.getSubject();
        requireNonBlank(subject, "JWT subject claim is required");
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new MalformedJwtException("JWT subject must be a valid user public ID", ex);
        }
    }

    private void requirePresent(Object value, String message) {
        if (value == null) throw new MalformedJwtException(message);
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new MalformedJwtException(message);
    }

    private void requireAccessTokenType(String actualValue) {
        if (!ACCESS_TOKEN_TYPE.equals(actualValue)) {
            throw new MalformedJwtException("JWT token type must be access");
        }
    }

    public record VerifiedAccessToken(UUID userPublicId, Instant expiresAt) {}
}
