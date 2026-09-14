package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyVerificationRepository extends JpaRepository<CompanyVerification, Long> {

    Optional<CompanyVerification> findByCompanyId(Long companyId);

    Page<CompanyVerification> findByStatusIn(List<CompanyVerification.VerificationStatus> statuses, Pageable pageable);
}
