package com.krino.backend.mapper;

import com.krino.backend.entity.RefreshToken;
import com.krino.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenMapperTest
{
    private RefreshTokenMapper refreshTokenMapper;

    @BeforeEach
    void setUp()
    {
        refreshTokenMapper = Mappers.getMapper(RefreshTokenMapper.class);
    }

    @Test
    void toEntity_copiesAllFieldsAndStartsUnconsumedAndUnrevoked()
    {
        User user = new User();
        byte[] tokenHash = {1, 2, 3, 4};
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Instant createdAt = Instant.now();

        RefreshToken token = refreshTokenMapper.toEntity(user, tokenHash, expiresAt, createdAt, "Firefox on Linux",
                "127.0.0.1");

        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getTokenHash()).isEqualTo(tokenHash);
        assertThat(token.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(token.getCreatedAt()).isEqualTo(createdAt);
        assertThat(token.getDeviceInfo()).isEqualTo("Firefox on Linux");
        assertThat(token.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(token.isConsumed()).isFalse();
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getId()).isNull();
    }
}
