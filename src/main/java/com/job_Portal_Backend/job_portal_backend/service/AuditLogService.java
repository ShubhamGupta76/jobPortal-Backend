package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.dto.AuditLogDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {

    void logAction(String entityType, Long entityId, String action, User user,
            String oldValues, String newValues, String description,
            String ipAddress, String userAgent);

    void logAction(String entityType, Long entityId, String action, User user,
            String description, String ipAddress, String userAgent);

    Page<AuditLogDto> getAuditLogsByUser(User user, Pageable pageable);

    Page<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable);

    Page<AuditLogDto> getAuditLogsByAction(String action, Pageable pageable);

    Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<AuditLogDto> getAuditLogsByUserAndDateRange(User user, LocalDateTime start, LocalDateTime end,
            Pageable pageable);

    Page<AuditLogDto> getAuditLogsByEntityAndDateRange(String entityType, LocalDateTime start, LocalDateTime end,
            Pageable pageable);

    long countActionsByUserSince(User user, String action, LocalDateTime since);

    List<String> getDistinctEntityTypes();

    List<String> getDistinctActions();

    List<AuditLogDto> getAuditLogsByIpAddress(String ipAddress);
}