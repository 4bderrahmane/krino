package com.jesa.interviewslotmanager.utility;

import com.jesa.interviewslotmanager.entity.CustomUserDetails;
import io.jsonwebtoken.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.stream.Collectors;


@Component
@Slf4j
public class JwtUtility
{

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.issuer:interview-slot-manager}")
    private String issuer;

    @Value("${app.jwt.access-token-expiration.ms}")
    private long accessTokenExpirationInMs;

    @PostConstruct
    protected void init()
    {
        if (secretKey.length() < 32)
        {
            log.warn("JWT secret key should be at least 32 characters");
        }
    }

    public String generateAccessToken(CustomUserDetails userDetails)
    {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationInMs);

        return Jwts.builder()
                .setSubject(userDetails.getId().toString())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer)
                .claim("email", userDetails.getEmail())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()))
                .claim("type", "access")
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public boolean validateToken(String token)
    {
        try
        {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token);
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
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e)
        {
            log.error("Could not parse claims from token: {}", e.getMessage());
            throw new JwtException("Invalid token", e);
        }
    }

    public Long getUserIdFromToken(String token)
    {
        Claims claims = getClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
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


    public String extractTokenFromRequest(HttpServletRequest request)
    {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer "))
        {
            return bearerToken.substring(7).trim();
        }

        return null;
    }
}