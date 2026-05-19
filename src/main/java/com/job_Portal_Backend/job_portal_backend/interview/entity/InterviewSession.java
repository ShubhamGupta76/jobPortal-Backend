package com.job_Portal_Backend.job_portal_backend.interview.entity;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions", indexes = {
        @Index(name = "idx_interview_session_room_token", columnList = "roomToken", unique = true),
        @Index(name = "idx_interview_session_invite_token", columnList = "inviteToken", unique = true)
})
@Getter
@Setter
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String roomToken;

    @Column(nullable = false, unique = true, length = 120)
    private String inviteToken;

    @Column(nullable = false, length = 160)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewRoundType roundType = InterviewRoundType.TECHNICAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewSessionStatus status = InterviewSessionStatus.SCHEDULED;

    @Column(nullable = false)
    private LocalDateTime scheduledStartAt;

    @Column(nullable = false)
    private LocalDateTime scheduledEndAt;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Column(nullable = false)
    private Integer durationMinutes = 60;

    @Column(nullable = false)
    private Boolean candidateCanJoinOnce = true;

    @Column(nullable = false)
    private Boolean recordingEnabled = false;

    @Column(nullable = false)
    private Boolean recordingRequested = false;

    @Column(length = 500)
    private String recordingStorageKey;

    @Column(nullable = false)
    private Boolean identityVerificationRequired = true;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewParticipant> participants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
