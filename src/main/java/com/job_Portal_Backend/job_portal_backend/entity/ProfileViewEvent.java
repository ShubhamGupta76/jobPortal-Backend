package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_view_events", indexes = {
        @Index(name = "idx_profile_view_events_viewed_user", columnList = "viewed_user_id, created_at")
})
@Getter
@Setter
@ToString(exclude = { "viewedUser", "viewedBy" })
public class ProfileViewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewed_user_id", nullable = false)
    private User viewedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewed_by_id", nullable = false)
    private User viewedBy;

    @Column(length = 30)
    private String source;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
