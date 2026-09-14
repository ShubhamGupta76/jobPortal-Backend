package com.job_Portal_Backend.job_portal_backend.session.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A session as shown to its owner. Never carries the JWT, the refresh token, or any hash of it —
 * only metadata the user needs to recognize and, if needed, revoke a session.
 */
@Data
@AllArgsConstructor
public class SessionDto {
    private Long id;
    private String deviceLabel;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private boolean current;
}
