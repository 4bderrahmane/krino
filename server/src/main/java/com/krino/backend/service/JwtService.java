package com.krino.backend.service;

import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.exception.TokenException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String EMAIL_CLAIM = "email";
    private static final String EXPECTED_ALGORITHM = "HS256";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-token-expiration}")
    private Duration accessTokenExpiration;

    private SecretKey signingKey;

    @PostConstruct
    protected void init() {
        if (secretKey == null || secretKey.length() < 32)
            throw new TokenException("Invalid JWT secret: it must be at least 32 characters for HS256.");

        if (issuer == null || issuer.isBlank())
            throw new TokenException("Invalid JWT issuer: it must not be blank.");

        if (accessTokenExpiration.isNegative() || accessTokenExpiration.isZero())
            throw new TokenException("Invalid access-token lifetime: it must be greater than zero.");

        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiration);

        return Jwts.builder()
                .subject(userDetails.getPublicId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .issuer(issuer)
                .claim(EMAIL_CLAIM, userDetails.getEmail())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    // Rejected tokens are expected traffic (expired sessions, malformed cookies), so they log
    // at debug; a bad signature or a foreign algorithm is a possible attack signal, so warn.
    public boolean validateToken(String token) {
        try {
            parseAndValidateClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.debug("Expired JWT token: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.debug("Invalid JWT claims: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.debug("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return parseAndValidateClaims(token);
        } catch (Exception e) {
            log.debug("Could not parse claims from token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public UUID getUserPublicIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get(EMAIL_CLAIM, String.class);
    }

    public long getAccessTokenExpiryInSeconds() {
        return accessTokenExpiration.toSeconds();
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Instant expiration = getExpirationFromToken(token).toInstant();
        return expiration.isBefore(Instant.now());
    }

    private Claims parseAndValidateClaims(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("JWT token must not be blank");
        }

        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);

        validateHeader(parsedToken);
        Claims claims = parsedToken.getPayload();
        validateAccessTokenClaims(claims);
        return claims;
    }

    private void validateHeader(Jws<Claims> parsedToken) {
        String algorithm = parsedToken.getHeader().getAlgorithm();
        if (!EXPECTED_ALGORITHM.equals(algorithm)) {
            throw new UnsupportedJwtException("Unsupported JWT signing algorithm: " + algorithm);
        }
    }

    private void validateAccessTokenClaims(Claims claims) {
        requireValidSubject(claims);
        requirePresent(claims.getIssuedAt(), "JWT issued-at claim is required");
        requirePresent(claims.getExpiration(), "JWT expiration claim is required");
        requireAccessTokenType(claims.get(TOKEN_TYPE_CLAIM, String.class));
        requireNonBlank(claims.get(EMAIL_CLAIM, String.class), "JWT email claim is required");
    }

    private void requireValidSubject(Claims claims) {
        String subject = claims.getSubject();
        requireNonBlank(subject, "JWT subject claim is required");
        try {
            UUID.fromString(subject);
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
}
