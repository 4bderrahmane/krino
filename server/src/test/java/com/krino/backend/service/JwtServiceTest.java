package com.krino.backend.service;

import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.TokenException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest
{
    private static final String SECRET = "test-signing-secret-that-is-long-enough-for-hs256-0123456789";
    private static final String ISSUER = "krino-test";
    private static final long ACCESS_TOKEN_TTL_MS = 900_000L;
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMillis(ACCESS_TOKEN_TTL_MS);

    private static final UUID PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "candidate@test.local";

    private JwtService jwtService;

    @BeforeEach
    void setUp()
    {
        jwtService = newJwtService(SECRET, ACCESS_TOKEN_TTL);
    }

    @Test
    void init_secretShorterThan32Chars_throwsTokenException()
    {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "too-short");
        ReflectionTestUtils.setField(service, "issuer", ISSUER);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", ACCESS_TOKEN_TTL);

        assertThatThrownBy(service::init)
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void init_blankIssuer_throwsTokenException()
    {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "issuer", " ");
        ReflectionTestUtils.setField(service, "accessTokenExpiration", ACCESS_TOKEN_TTL);

        assertThatThrownBy(service::init)
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void init_nonPositiveAccessTokenLifetime_throwsTokenException()
    {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "issuer", ISSUER);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", Duration.ZERO);

        assertThatThrownBy(service::init)
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void generateAccessToken_producesValidTokenCarryingSubjectAndEmail()
    {
        String token = jwtService.generateAccessToken(userDetails());

        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.getUserPublicIdFromToken(token)).isEqualTo(PUBLIC_ID);
        assertThat(jwtService.getEmailFromToken(token)).isEqualTo(EMAIL);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse()
    {
        String token = jwtService.generateAccessToken(userDetails());
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtService.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_garbageString_returnsFalse()
    {
        assertThat(jwtService.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    void validateToken_tokenSignedWithDifferentKey_returnsFalse()
    {
        JwtService otherService = newJwtService("another-completely-different-secret-key-0123456789abcd", ACCESS_TOKEN_TTL);
        String foreignToken = otherService.generateAccessToken(userDetails());

        assertThat(jwtService.validateToken(foreignToken)).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse()
    {
        String expiredToken = accessToken(builderNow(-2_000L), builderNow(-1_000L));

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
    }

    @Test
    void validateToken_wrongIssuer_returnsFalse()
    {
        String token = accessToken("different-issuer", PUBLIC_ID.toString(), builderNow(), builderNow(ACCESS_TOKEN_TTL_MS),
                "access", EMAIL);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_missingTokenType_returnsFalse()
    {
        String token = accessToken(ISSUER, PUBLIC_ID.toString(), builderNow(), builderNow(ACCESS_TOKEN_TTL_MS), null,
                EMAIL);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_nonAccessTokenType_returnsFalse()
    {
        String token = accessToken(ISSUER, PUBLIC_ID.toString(), builderNow(), builderNow(ACCESS_TOKEN_TTL_MS),
                "refresh", EMAIL);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_missingExpiration_returnsFalse()
    {
        String token = accessToken(ISSUER, PUBLIC_ID.toString(), builderNow(), null, "access", EMAIL);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_missingIssuedAt_returnsFalse()
    {
        String token = accessToken(ISSUER, PUBLIC_ID.toString(), null, builderNow(ACCESS_TOKEN_TTL_MS), "access",
                EMAIL);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_missingEmail_returnsFalse()
    {
        String token = accessToken(ISSUER, PUBLIC_ID.toString(), builderNow(), builderNow(ACCESS_TOKEN_TTL_MS),
                "access", null);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_subjectIsNotUserPublicId_returnsFalse()
    {
        String token = accessToken(ISSUER, "123", builderNow(), builderNow(ACCESS_TOKEN_TTL_MS), "access", EMAIL);

        assertThat(jwtService.validateToken(token)).isFalse();
    }

    @Test
    void getClaimsFromToken_invalidToken_throwsJwtException()
    {
        assertThatThrownBy(() -> jwtService.getClaimsFromToken("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void getAccessTokenExpiryInSeconds_convertsMillisToSeconds()
    {
        assertThat(jwtService.getAccessTokenExpiryInSeconds()).isEqualTo(900L);
    }

    private JwtService newJwtService(String secret, Duration ttl)
    {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "issuer", ISSUER);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", ttl);
        service.init();
        return service;
    }

    private CustomUserDetails userDetails()
    {
        User user = User.builder()
                .publicId(PUBLIC_ID)
                .email(EMAIL)
                .roles(Set.of(UserRole.CANDIDATE))
                .build();
        return new CustomUserDetails(user);
    }

    private String accessToken(Date issuedAt, Date expiration)
    {
        return accessToken(ISSUER, PUBLIC_ID.toString(), issuedAt, expiration, "access", EMAIL);
    }

    private String accessToken(String issuer, String subject, Date issuedAt, Date expiration, String tokenType,
                               String email)
    {
        JwtBuilder builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claim("roles", List.of("ROLE_CANDIDATE"));

        if (issuedAt != null) {
            builder.issuedAt(issuedAt);
        }
        if (expiration != null) {
            builder.expiration(expiration);
        }
        if (tokenType != null) {
            builder.claim("type", tokenType);
        }
        if (email != null) {
            builder.claim("email", email);
        }

        return builder
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey signingKey()
    {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private Date builderNow()
    {
        return builderNow(0);
    }

    private Date builderNow(long offsetMs)
    {
        return new Date(System.currentTimeMillis() + offsetMs);
    }
}
