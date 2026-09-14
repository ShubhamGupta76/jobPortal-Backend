package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Report;
import com.job_Portal_Backend.job_portal_backend.entity.Report.ReportStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Report.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);

    Page<Report> findByTargetType(TargetType targetType, Pageable pageable);

    Page<Report> findByStatusAndTargetType(ReportStatus status, TargetType targetType, Pageable pageable);

    boolean existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
            Long reporterId, TargetType targetType, Long targetId, LocalDateTime since);
}
