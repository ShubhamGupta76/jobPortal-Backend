package com.job_Portal_Backend.job_portal_backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_verification_documents", indexes = {
        @Index(name = "idx_cv_documents_verification_id", columnList = "verification_id")
})
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "verification", "fileUpload" })
public class CompanyVerificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private CompanyVerification verification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_upload_id", nullable = false)
    private FileUpload fileUpload;

    private String label;

    @Column(nullable = false, updatable = false)
    private LocalDateTime attachedAt;

    @PrePersist
    protected void onCreate() {
        attachedAt = LocalDateTime.now();
    }
}
