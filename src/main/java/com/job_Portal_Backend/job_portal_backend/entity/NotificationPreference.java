package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A user's explicit override for one notification category. The ABSENCE of a row for a given
 * (user, category) pair is meaningful, not an error: it means "use the default for this
 * category" (see NotificationCategoryDefaults), which is what preserves every existing user's
 * current notification behavior until they explicitly change a setting.
 */
@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_preference_user_category", columnNames = { "user_id", "category" })
}, indexes = {
        @Index(name = "idx_notification_preferences_user_id", columnList = "user_id")
})
@Getter
@Setter
@ToString(exclude = "user")
public class NotificationPreference {

    public enum Category {
        APPLICATION_UPDATES,
        MESSAGES,
        INTERVIEW_REMINDERS,
        JOB_ALERTS,
        RECRUITER_ACTIVITY,
        COMPANY_VERIFICATION,
        REPORTS,
        SYSTEM,
        BILLING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled;

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
