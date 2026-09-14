package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "dismissed_jobs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dismissed_job_user_job", columnNames = { "user_id", "job_id" })
}, indexes = {
        @Index(name = "idx_dismissed_jobs_user_id", columnList = "user_id")
})
@Getter
@Setter
@ToString(exclude = { "user", "job" })
public class DismissedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
