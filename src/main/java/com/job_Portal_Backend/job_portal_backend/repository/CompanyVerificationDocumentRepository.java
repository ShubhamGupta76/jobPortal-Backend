package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyVerificationDocumentRepository extends JpaRepository<CompanyVerificationDocument, Long> {

    List<CompanyVerificationDocument> findByVerificationIdOrderByAttachedAtDesc(Long verificationId);

    void deleteByVerificationId(Long verificationId);
}
