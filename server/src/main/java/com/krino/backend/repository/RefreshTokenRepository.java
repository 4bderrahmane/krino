package com.krino.backend.repository;

import com.krino.backend.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID>
{

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.consumed = false AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidTokenByHash(@Param("tokenHash") byte[] tokenHash, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.consumed = false AND rt.revoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidTokenByHashForUpdate(@Param("tokenHash") byte[] tokenHash, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.id = :id")
    void revokeToken(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    void revokeAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    void deleteExpiredAndRevokedTokens(@Param("now") Instant now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.consumed = false AND rt.revoked = false AND rt.expiresAt > :now ORDER BY rt.createdAt DESC")
    List<RefreshToken> findActiveTokensByUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.consumed = false AND rt.revoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUser(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.consumed = false AND rt.revoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findAllValidTokens(@Param("now") Instant now);

    List<RefreshToken> findByUserId(Long userId);

    List<RefreshToken> findByRevokedAndExpiresAtBefore(boolean revoked, Instant expiryDate);

    void deleteAllByUserId(Long userId);
}
