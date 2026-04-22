package com.job_Portal_Backend.job_portal_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String entityType;
    private Long entityId;
    private String action;
    private String oldValues;
    private String newValues;
    private String description;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime timestamp;
}