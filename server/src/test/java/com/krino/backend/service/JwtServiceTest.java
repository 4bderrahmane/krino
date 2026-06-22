package com.krino.backend.service;

import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.TokenException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest
{
    private static final String SECRET = "test-signing-secret-that-is-long-enough-for-hs256-0123456789";
    private static final String ISSUER = "krino-test";
    private static final long ACCESS_TOKEN_TTL_MS = 900_000L;

    private static final UUID PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String EMAIL = "candidate@test.local";

    private JwtService jwtService;

    @BeforeEach
    void setUp()
    {
        jwtService = newJwtService(SECRET, ACCESS_TOKEN_TTL_MS);
    }

    @Test
    void init_secretShorterThan32Chars_throwsTokenException()
    {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "too-short");
        ReflectionTestUtils.setField(service, "issuer", ISSUER);
        ReflectionTestUtils.setField(service, "accessTokenExpirationInMs", ACCESS_TOKEN_TTL_MS);

        assertThatThrownBy(service::init)
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("at least 32 characters");
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
        JwtService otherService = newJwtService("another-completely-different-secret-key-0123456789abcd", ACCESS_TOKEN_TTL_MS);
        String foreignToken = otherService.generateAccessToken(userDetails());

        assertThat(jwtService.validateToken(foreignToken)).isFalse();
    }

    @Test
    void validateToken_expiredToken_returnsFalse()
    {
        JwtService expiringService = newJwtService(SECRET, -1_000L);
        String expiredToken = expiringService.generateAccessToken(userDetails());

        assertThat(jwtService.validateToken(expiredToken)).isFalse();
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

    private JwtService newJwtService(String secret, long ttlMs)
    {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "issuer", ISSUER);
        ReflectionTestUtils.setField(service, "accessTokenExpirationInMs", ttlMs);
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
}
