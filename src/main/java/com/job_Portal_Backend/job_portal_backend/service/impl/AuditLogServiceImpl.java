package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.dto.AuditLogDto;
import com.job_Portal_Backend.job_portal_backend.entity.AuditLog;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.AuditLogRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void logAction(String entityType, Long entityId, String action, User user,
            String oldValues, String newValues, String description,
            String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setUser(user);
        auditLog.setUserEmail(user.getEmail());
        auditLog.setOldValues(oldValues);
        auditLog.setNewValues(newValues);
        auditLog.setDescription(description);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);

        auditLogRepository.save(auditLog);
    }

    @Override
    public void logAction(String entityType, Long entityId, String action, User user,
            String description, String ipAddress, String userAgent) {
        logAction(entityType, entityId, action, user, null, null, description, ipAddress, userAgent);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByUser(User user, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByUser(user, pageable);
        return auditLogs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByEntity(String entityType, Long entityId, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return auditLogs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByAction(String action, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByAction(action, pageable);
        return auditLogs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByTimestampBetween(start, end, pageable);
        return auditLogs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByUserAndDateRange(User user, LocalDateTime start, LocalDateTime end,
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByUserAndDateRange(user, start, end, pageable);
        return auditLogs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogsByEntityAndDateRange(String entityType, LocalDateTime start, LocalDateTime end,
            Pageable pageable) {
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndDateRange(entityType, start, end, pageable);
        return auditLogs.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public long countActionsByUserSince(User user, String action, LocalDateTime since) {
        return auditLogRepository.countActionsByUserSince(user, action, since);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctEntityTypes() {
        return auditLogRepository.findDistinctEntityTypes();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistinctActions() {
        return auditLogRepository.findDistinctActions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> getAuditLogsByIpAddress(String ipAddress) {
        List<AuditLog> auditLogs = auditLogRepository.findByIpAddress(ipAddress);
        return auditLogs.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private AuditLogDto mapToDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUser() != null ? auditLog.getUser().getId() : null)
                .userEmail(auditLog.getUserEmail())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .oldValues(auditLog.getOldValues())
                .newValues(auditLog.getNewValues())
                .description(auditLog.getDescription())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}