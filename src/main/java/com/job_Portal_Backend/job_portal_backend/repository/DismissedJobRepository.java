package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.DismissedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DismissedJobRepository extends JpaRepository<DismissedJob, Long> {

    List<DismissedJob> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<DismissedJob> findByUserIdAndJobId(Long userId, Long jobId);

    boolean existsByUserIdAndJobId(Long userId, Long jobId);
}
