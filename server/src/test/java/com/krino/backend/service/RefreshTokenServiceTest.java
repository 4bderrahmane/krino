package com.krino.backend.service;

import com.krino.backend.configuration.properties.AuthenticationProperties;
import com.krino.backend.entity.RefreshToken;
import com.krino.backend.entity.User;
import com.krino.backend.mapper.RefreshTokenMapper;
import com.krino.backend.repository.RefreshTokenRepository;
import com.krino.backend.security.TokenHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest
{
    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp()
    {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        // A plain SecureRandom keeps the unit test off the blocking /dev/random source.
        TokenHasher tokenHasher = new TokenHasher(new SecureRandom());
        ReflectionTestUtils.setField(tokenHasher, "hmacSecret", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.invokeMethod(tokenHasher, "init");
        refreshTokenService = new RefreshTokenService(refreshTokenRepository,
                Mappers.getMapper(RefreshTokenMapper.class), tokenHasher, authenticationProperties());
    }

    @Test
    void generateAndSaveRefreshTokenStoresOnlyHmacHash()
    {
        User user = new User();

        String rawToken = refreshTokenService.generateAndSaveRefreshToken(user, "JUnit", "203.0.113.20");

        org.mockito.ArgumentCaptor<RefreshToken> captor = org.mockito.ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(rawToken).isNotBlank();
        assertThat(saved.getTokenHash()).hasSize(32);
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken.getBytes(StandardCharsets.UTF_8));
        assertThat(refreshTokenService.validateToken(rawToken, saved.getTokenHash())).isTrue();
        assertThat(saved.getDeviceInfo()).isEqualTo("JUnit");
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.20");
        assertThat(saved.isRevoked()).isFalse();
        assertThat(Duration.between(saved.getCreatedAt(), saved.getExpiresAt())).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void findValidRefreshTokenRejectsBlankTokenWithoutRepositoryLookup()
    {
        assertThat(refreshTokenService.findValidRefreshToken(" ")).isEmpty();

        verify(refreshTokenRepository, never()).findValidTokenByHash(any(), any());
    }

    @Test
    void findValidRefreshTokenLooksUpByHmac()
    {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findValidTokenByHash(any(), any())).thenReturn(Optional.of(token));

        assertThat(refreshTokenService.findValidRefreshToken("raw-token")).contains(token);

        verify(refreshTokenRepository).findValidTokenByHash(any(byte[].class), any());
    }

    private AuthenticationProperties authenticationProperties()
    {
        return new AuthenticationProperties(
                "krino-test",
                "0123456789abcdef0123456789abcdef",
                Duration.ofMinutes(15),
                Duration.ofDays(30),
                "access_token",
                "refresh_token"
        );
    }
}
