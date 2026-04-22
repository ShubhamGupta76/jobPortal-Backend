package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_uploads", indexes = {
        @Index(name = "idx_file_uploads_user_id", columnList = "user_id"),
        @Index(name = "idx_file_uploads_entity_type", columnList = "entity_type"),
        @Index(name = "idx_file_uploads_uploaded_at", columnList = "uploaded_at")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "user")
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255, unique = true)
    private String storedFilename;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // RESUME, PROFILE_PICTURE, JOB_ATTACHMENT, etc.

    @Column(name = "entity_id")
    private Long entityId; // ID of the related entity (job, application, etc.)

    @Column(name = "file_category", length = 50)
    private String fileCategory; // DOCUMENT, IMAGE, VIDEO, etc.

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isDeleted'");
    }
}