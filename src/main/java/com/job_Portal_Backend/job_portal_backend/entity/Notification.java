package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;

    @Column(length = 2000)
    private String message;

    private String type;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    private LocalDateTime readAt;

    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isRead() {
        return Boolean.TRUE.equals(read);
    }

    public void setRead(Boolean read) {
        this.read = read;
    }
}
