package com.job_Portal_Backend.job_portal_backend.assessments.entity;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(nullable = false, unique = true)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.NOT_STARTED;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Integer attemptsCount = 0;

    @Column(nullable = false)
    private Boolean completedSuccessfully = false;

    @Column(columnDefinition = "TEXT")
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String deviceFingerprint;

    @Column(nullable = false)
    private Integer fullscreenViolationCount = 0;

    @Column(nullable = false)
    private Integer tabSwitchCount = 0;

    @Column(nullable = false)
    private Integer securityViolationCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum SessionStatus {
        NOT_STARTED, IN_PROGRESS, SUBMITTED, EVALUATED, EXPIRED
    }
}
