package com.job_Portal_Backend.job_portal_backend.interview.entity;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uk_interview_participant_session_user", columnNames = { "session_id", "user_id" })
})
@Getter
@Setter
public class InterviewParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewParticipantStatus status = InterviewParticipantStatus.INVITED;

    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private LocalDateTime lastSeenAt;

    @Column(nullable = false)
    private Integer joinCount = 0;

    @Column(nullable = false)
    private Boolean microphoneMuted = false;

    @Column(nullable = false)
    private Boolean cameraOff = false;

    @Column(nullable = false)
    private Boolean screenSharing = false;

    @Column(length = 500)
    private String deviceMetadata;

    @Column(length = 120)
    private String networkQuality = "unknown";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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
