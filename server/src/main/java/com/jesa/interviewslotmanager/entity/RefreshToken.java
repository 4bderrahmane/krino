package com.jesa.interviewslotmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_expires_at", columnList = "expiresAt")
})
public class RefreshToken
{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, unique = true, columnDefinition = "BINARY(48)") // 16 (salt) + 32 (sha256) = 48 bytes
    private byte[] tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime lastUsedAt;

    private String deviceInfo;

    private String ipAddress;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRevoked = false;

    // Utility methods
    public boolean isExpired()
    {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public boolean isValid()
    {
        return !isRevoked && !isExpired();
    }

    public void markUsed()
    {
        this.lastUsedAt = LocalDateTime.now();
    }

    // PrePersist callback to set createdAt if not already set
    @PrePersist
    protected void onCreate()
    {
        if (this.createdAt == null)
        {
            this.createdAt = LocalDateTime.now();
        }
    }
}