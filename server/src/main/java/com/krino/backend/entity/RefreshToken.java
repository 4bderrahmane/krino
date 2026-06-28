package com.krino.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * HMAC-SHA256 of the raw token using the server-side refresh-token secret.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 32)
    private byte[] tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Set true when this token is rotated away. A consumed token reappearing = reuse.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean consumed = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;

    // optional audit context — fine to keep
    private String deviceInfo;
    private String ipAddress;

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    /**
     * Usable only if untouched, unrevoked, unexpired.
     */
    public boolean isUsable() {
        return !consumed && !revoked && !isExpired();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
