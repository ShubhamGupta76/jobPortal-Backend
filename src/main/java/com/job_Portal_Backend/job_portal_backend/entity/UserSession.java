package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A refresh-token-backed session, tracked alongside (not instead of) the existing stateless JWT
 * access token. Only SHA-256 hashes of the opaque refresh token values are ever persisted here —
 * never the raw token. previousRefreshTokenHash retains the immediately-prior hash for one
 * rotation cycle so a replayed (already-rotated) refresh token can be detected as reuse.
 */
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_user_sessions_user_id", columnList = "user_id"),
        @Index(name = "idx_user_sessions_refresh_token_hash", columnList = "refresh_token_hash"),
        @Index(name = "idx_user_sessions_previous_refresh_token_hash", columnList = "previous_refresh_token_hash")
})
@Getter
@Setter
@ToString(exclude = { "user", "refreshTokenHash", "previousRefreshTokenHash" })
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 64, unique = true)
    private String refreshTokenHash;

    @Column(name = "previous_refresh_token_hash", length = 64)
    private String previousRefreshTokenHash;

    @Column(name = "device_label", length = 255)
    private String deviceLabel;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_reason", length = 40)
    private String revokedReason;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastUsedAt == null) {
            lastUsedAt = createdAt;
        }
    }

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(LocalDateTime.now());
    }
}
