package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "job", "candidate", "recruiter" })
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private User recruiter;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type; // TECHNICAL, HR, SYSTEM_DESIGN, CODING

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status; // SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW

    @Column(length = 1000)
    private String notes;

    @Column(length = 500)
    private String meetingLink; // Zoom, Google Meet, etc.

    @Column(length = 500)
    private String location; // For in-person interviews

    private Integer durationMinutes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum InterviewType {
        TECHNICAL, HR, SYSTEM_DESIGN, CODING, BEHAVIORAL
    }

    public enum InterviewStatus {
        SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
    }
}