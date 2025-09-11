package com.jesa.interviewslotmanager.repository;

import com.jesa.interviewslotmanager.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID>
{

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.tokenHash = :tokenHash AND rt.isRevoked = false AND rt.expiresAt > :now")
    Optional<RefreshToken> findValidTokenByHash(@Param("tokenHash") byte[] tokenHash, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.lastUsedAt = :now WHERE rt.id = :id")
    void updateLastUsed(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.id = :id")
    void revokeToken(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user.id = :userId")
    void revokeAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.isRevoked = true")
    void deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.isRevoked = false AND rt.expiresAt > :now ORDER BY rt.lastUsedAt DESC")
    List<RefreshToken> findActiveTokensByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(rt) FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countActiveTokensByUser(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Query("SELECT rt FROM RefreshToken rt WHERE rt.isRevoked = false AND rt.expiresAt > :now")
    List<RefreshToken> findAllValidTokens(@Param("now") LocalDateTime now);

    List<RefreshToken> findByUserId(Long userId);

    List<RefreshToken> findByIsRevokedAndExpiresAtBefore(Boolean isRevoked, LocalDateTime expiryDate);

    void deleteAllByUserId(Long userId);
}
