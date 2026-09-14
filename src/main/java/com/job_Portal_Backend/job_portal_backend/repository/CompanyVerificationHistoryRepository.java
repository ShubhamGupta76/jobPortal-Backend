package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyVerificationHistoryRepository extends JpaRepository<CompanyVerificationHistory, Long> {

    List<CompanyVerificationHistory> findByVerificationIdOrderByCreatedAtAsc(Long verificationId);
}
