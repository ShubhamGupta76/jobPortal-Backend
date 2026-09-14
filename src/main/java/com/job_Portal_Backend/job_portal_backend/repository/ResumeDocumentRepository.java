package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.ResumeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeDocumentRepository extends JpaRepository<ResumeDocument, Long> {

    List<ResumeDocument> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ResumeDocument> findByIdAndUserId(Long id, Long userId);

    Optional<ResumeDocument> findByUserIdAndIsPrimaryTrue(Long userId);

    long countByUserId(Long userId);
}
