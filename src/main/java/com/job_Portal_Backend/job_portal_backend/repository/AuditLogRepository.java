package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.AuditLog;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUser(User user, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.user = :user AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserAndDateRange(@Param("user") User user, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEntityTypeAndDateRange(@Param("entityType") String entityType,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user = :user AND a.action = :action AND a.timestamp >= :since")
    long countActionsByUserSince(@Param("user") User user, @Param("action") String action,
            @Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a WHERE a.ipAddress = :ipAddress ORDER BY a.timestamp DESC")
    List<AuditLog> findByIpAddress(@Param("ipAddress") String ipAddress);

    @Query("SELECT DISTINCT a.entityType FROM AuditLog a")
    List<String> findDistinctEntityTypes();

    @Query("SELECT DISTINCT a.action FROM AuditLog a")
    List<String> findDistinctActions();
}