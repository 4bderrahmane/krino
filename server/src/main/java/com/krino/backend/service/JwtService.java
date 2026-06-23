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
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Value("${app.jwt.access-token-expiration.ms}")
    private long accessTokenExpirationInMs;

    private SecretKey signingKey;

    @PostConstruct
    protected void init() {
        if (secretKey == null || secretKey.length() < 32) {
            throw new TokenException("Invalid JWT secret: it must be at least 32 characters for HS256.");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new TokenException("Invalid JWT issuer: it must not be blank.");
        }
        if (accessTokenExpirationInMs <= 0) {
            throw new TokenException("Invalid access-token lifetime: it must be greater than zero.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationInMs);

        return Jwts.builder()
                .subject(userDetails.getPublicId().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(issuer)
                .claim("email", userDetails.getEmail())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            parseAndValidateClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.error("Invalid JWT claims: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public Claims getClaimsFromToken(String token) {
        try {
            return parseAndValidateClaims(token);
        } catch (Exception e) {
            log.error("Could not parse claims from token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public UUID getUserPublicIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    public long getAccessTokenExpiryInSeconds() {
        return accessTokenExpirationInMs / 1000;
    }

    public Date getExpirationFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationFromToken(token);
        return expiration.before(new Date());
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
        requireClaimValue(ACCESS_TOKEN_TYPE, claims.get(TOKEN_TYPE_CLAIM, String.class),
                "JWT token type must be access");
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
        if (value == null) {
            throw new MalformedJwtException(message);
        }
    }

    private void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new MalformedJwtException(message);
        }
    }

    private void requireClaimValue(String expectedValue, String actualValue, String message) {
        if (!expectedValue.equals(actualValue)) {
            throw new MalformedJwtException(message);
        }
    }
}
