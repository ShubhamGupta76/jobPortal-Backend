package com.job_Portal_Backend.job_portal_backend.assessments.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "proctor_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_session_id", nullable = false)
    private TestSession testSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuspiciousActivity activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer severityScore = 0; // 0-100

    @Column(columnDefinition = "TEXT")
    private String metadata;

    private String screenshotUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum SuspiciousActivity {
        TAB_SWITCH, WINDOW_BLUR, COPY_PASTE_DETECTED, MULTIPLE_WINDOWS,
        NETWORK_CHANGE, DEVICE_DISCONNECT, SCREEN_SHARE, KEYBOARD_INACTIVE,
        CAMERA_DETECTED_MISSING, BACKGROUND_NOISE, FACE_NOT_DETECTED
    }
}
