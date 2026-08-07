package com.krino.backend.service;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import com.krino.backend.entity.CustomUserDetails;
import com.krino.backend.entity.User;
import com.krino.backend.entity.enums.UserRole;
import com.krino.backend.exception.TokenException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {
    private static final String SECRET = "test-signing-secret-that-is-long-enough-for-hs256-0123456789";
    private static final String ISSUER = "krino-test";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private static final UUID PUBLIC_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = newJwtService(SECRET, ISSUER, ACCESS_TOKEN_TTL);
    }

    @Test
    void init_secretShorterThan32Chars_throwsTokenException() {
        JwtService service = new JwtService(properties("too-short", ISSUER, ACCESS_TOKEN_TTL));

        assertThatThrownBy(service::init)
                .isInstanceOf(TokenException.class)
                .hasMessageContaining("at least 32 characters");
    }

    @Test
    void generateAccessToken_producesVerifiedAccessTokenForUser() {
        Instant beforeGeneration = Instant.now();

        String token = jwtService.generateAccessToken(userDetails());
        JwtService.VerifiedAccessToken verifiedToken = jwtService.parseAccessToken(token);

        assertThat(verifiedToken.userPublicId()).isEqualTo(PUBLIC_ID);
        assertThat(verifiedToken.expiresAt())
                .isBetween(beforeGeneration.plus(ACCESS_TOKEN_TTL).minusSeconds(1),
                        Instant.now().plus(ACCESS_TOKEN_TTL).plusSeconds(1));
    }

    @Test
    void parseAccessToken_tamperedToken_throwsBadCredentialsException() {
        String token = jwtService.generateAccessToken(userDetails());
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, signatureStart) + replacement + token.substring(signatureStart + 1);

        assertRejected(tampered);
    }

    @Test
    void parseAccessToken_garbageString_throwsBadCredentialsException() {
        assertRejected("not-a-jwt");
    }

    @Test
    void parseAccessToken_tokenSignedWithDifferentKey_throwsBadCredentialsException() {
        JwtService otherService = newJwtService(
                "another-completely-different-secret-key-0123456789abcd",
                ISSUER,
                ACCESS_TOKEN_TTL
        );

        assertRejected(otherService.generateAccessToken(userDetails()));
    }

    @Test
    void parseAccessToken_expiredToken_throwsBadCredentialsException() {
        assertRejected(accessToken(ISSUER, PUBLIC_ID.toString(), dateFromNow(-2_000), dateFromNow(-1_000), "access"));
    }

    @Test
    void parseAccessToken_wrongIssuer_throwsBadCredentialsException() {
        assertRejected(accessToken(
                "different-issuer",
                PUBLIC_ID.toString(),
                dateFromNow(0),
                dateFromNow(ACCESS_TOKEN_TTL.toMillis()),
                "access"
        ));
    }

    @Test
    void parseAccessToken_missingTokenType_throwsBadCredentialsException() {
        assertRejected(accessToken(
                ISSUER,
                PUBLIC_ID.toString(),
                dateFromNow(0),
                dateFromNow(ACCESS_TOKEN_TTL.toMillis()),
                null
        ));
    }

    @Test
    void parseAccessToken_nonAccessTokenType_throwsBadCredentialsException() {
        assertRejected(accessToken(
                ISSUER,
                PUBLIC_ID.toString(),
                dateFromNow(0),
                dateFromNow(ACCESS_TOKEN_TTL.toMillis()),
                "refresh"
        ));
    }

    @Test
    void parseAccessToken_missingExpiration_throwsBadCredentialsException() {
        assertRejected(accessToken(ISSUER, PUBLIC_ID.toString(), dateFromNow(0), null, "access"));
    }

    @Test
    void parseAccessToken_missingIssuedAt_throwsBadCredentialsException() {
        assertRejected(accessToken(
                ISSUER,
                PUBLIC_ID.toString(),
                null,
                dateFromNow(ACCESS_TOKEN_TTL.toMillis()),
                "access"
        ));
    }

    @Test
    void parseAccessToken_subjectIsNotUserPublicId_throwsBadCredentialsException() {
        assertRejected(accessToken(
                ISSUER,
                "123",
                dateFromNow(0),
                dateFromNow(ACCESS_TOKEN_TTL.toMillis()),
                "access"
        ));
    }

    @Test
    void parseAccessToken_blankToken_throwsBadCredentialsException() {
        assertRejected(" ");
    }

    @Test
    void getAccessTokenExpiryInSeconds_usesConfiguredLifetime() {
        assertThat(jwtService.getAccessTokenExpiryInSeconds()).isEqualTo(900L);
    }

    private void assertRejected(String token) {
        assertThatThrownBy(() -> jwtService.parseAccessToken(token))
                .isInstanceOf(BadCredentialsException.class);
    }

    private JwtService newJwtService(String secret, String issuer, Duration accessTtl) {
        JwtService service = new JwtService(properties(secret, issuer, accessTtl));
        service.init();
        return service;
    }

    private AuthenticationProperties properties(String secret, String issuer, Duration accessTtl) {
        return new AuthenticationProperties(
                issuer,
                secret,
                accessTtl,
                REFRESH_TOKEN_TTL,
                "access_token",
                "refresh_token"
        );
    }

    private CustomUserDetails userDetails() {
        User user = User.builder()
                .publicId(PUBLIC_ID)
                .email("candidate@test.local")
                .roles(Set.of(UserRole.CANDIDATE))
                .build();
        return new CustomUserDetails(user);
    }

    private String accessToken(String issuer,
                               String subject,
                               Date issuedAt,
                               Date expiration,
                               String tokenType) {
        JwtBuilder builder = Jwts.builder()
                .issuer(issuer)
                .subject(subject);

        if (issuedAt != null) {
            builder.issuedAt(issuedAt);
        }
        if (expiration != null) {
            builder.expiration(expiration);
        }
        if (tokenType != null) {
            builder.claim("type", tokenType);
        }

        return builder
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private Date dateFromNow(long offsetMs) {
        return new Date(System.currentTimeMillis() + offsetMs);
    }
}
