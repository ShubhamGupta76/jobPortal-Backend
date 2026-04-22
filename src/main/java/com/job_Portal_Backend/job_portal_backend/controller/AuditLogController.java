package com.job_Portal_Backend.job_portal_backend.controller;

import com.job_Portal_Backend.job_portal_backend.dto.AuditLogDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/my-activity")
    public ResponseEntity<Page<AuditLogDto>> getMyActivity(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByUser(user, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<Page<AuditLogDto>> getEntityAuditLogs(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByEntity(entityType, entityId, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/action/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getActionAuditLogs(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByAction(action, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getAuditLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLogDto> auditLogs = auditLogService.getAuditLogsByDateRange(start, end, pageable);
        return ResponseEntity.ok(auditLogs);
    }

    @GetMapping("/user/{userId}/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogDto>> getUserAuditLogsByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Note: In a real implementation, you'd need to fetch the User entity first
        // For now, this is a simplified version
        Pageable pageable = PageRequest.of(page, size);
        // This would need to be implemented properly in the service
        return ResponseEntity.ok(Page.empty());
    }

    @GetMapping("/entity-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getEntityTypes() {
        List<String> entityTypes = auditLogService.getDistinctEntityTypes();
        return ResponseEntity.ok(entityTypes);
    }

    @GetMapping("/actions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getActions() {
        List<String> actions = auditLogService.getDistinctActions();
        return ResponseEntity.ok(actions);
    }

    @GetMapping("/ip/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLogDto>> getAuditLogsByIpAddress(@PathVariable String ipAddress) {
        List<AuditLogDto> auditLogs = auditLogService.getAuditLogsByIpAddress(ipAddress);
        return ResponseEntity.ok(auditLogs);
    }
}