package com.krino.backend.mapper;

import com.krino.backend.entity.RefreshToken;
import com.krino.backend.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;

@Mapper(config = MapperConfiguration.class)
public interface RefreshTokenMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "consumed", constant = "false")
    @Mapping(target = "revoked", constant = "false")
    @Mapping(target = "deviceInfo", source = "deviceInfo")
    @Mapping(target = "ipAddress", source = "ipAddress")
    RefreshToken toEntity(User user, byte[] tokenHash, Instant expiresAt, Instant createdAt, String deviceInfo,
                          String ipAddress);
}
