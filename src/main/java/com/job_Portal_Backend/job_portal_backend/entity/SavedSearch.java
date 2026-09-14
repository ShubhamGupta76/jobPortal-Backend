package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "saved_searches", indexes = {
        @Index(name = "idx_saved_searches_user_enabled", columnList = "user_id, enabled")
})
@Getter
@Setter
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 120)
    private String name;

    private String keyword;
    private String location;
    private String skills;
    private String experienceLevel;
    private String jobType;
    private String workplaceType;
    private Double minSalary;
    private Double maxSalary;

    @Column(nullable = false, length = 20)
    private String frequency = "IMMEDIATE";

    @Column(nullable = false)
    private Boolean enabled = true;

    private LocalDateTime lastMatchedAt;
    private LocalDateTime lastDeliveredAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (frequency == null || frequency.isBlank()) frequency = "IMMEDIATE";
        if (enabled == null) enabled = true;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}