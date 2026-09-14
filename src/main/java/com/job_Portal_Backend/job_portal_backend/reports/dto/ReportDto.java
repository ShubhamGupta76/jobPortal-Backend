package com.job_Portal_Backend.job_portal_backend.reports.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportDto {
    private Long id;
    private String targetType;
    private Long targetId;
    private String targetSummary;
    private String reportType;
    private String description;
    private String status;
    private String reporterName;
    private String reporterEmail;
    private String resolvedByName;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private LocalDateTime createdAt;
}
