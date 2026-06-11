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
public class JwtService
{

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.access-token-expiration.ms}")
    private long accessTokenExpirationInMs;

    private SecretKey signingKey;

    @PostConstruct
    protected void init()
    {
        if (secretKey == null || secretKey.length() < 32)
        {
            throw new TokenException("Invalid JWT secret: it must be at least 32 characters for HS256.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(CustomUserDetails userDetails)
    {
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
                .claim("type", "access")
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token)
    {
        try
        {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex)
        {
            log.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex)
        {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex)
        {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex)
        {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex)
        {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    public Claims getClaimsFromToken(String token)
    {
        try
        {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e)
        {
            log.error("Could not parse claims from token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public UUID getUserPublicIdFromToken(String token)
    {
        Claims claims = getClaimsFromToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token)
    {
        Claims claims = getClaimsFromToken(token);
        return claims.get("email", String.class);
    }

    public long getAccessTokenExpiryInSeconds()
    {
        return accessTokenExpirationInMs / 1000;
    }

    public Date getExpirationFromToken(String token)
    {
        Claims claims = getClaimsFromToken(token);
        return claims.getExpiration();
    }

    public boolean isTokenExpired(String token)
    {
        Date expiration = getExpirationFromToken(token);
        return expiration.before(new Date());
    }
}
