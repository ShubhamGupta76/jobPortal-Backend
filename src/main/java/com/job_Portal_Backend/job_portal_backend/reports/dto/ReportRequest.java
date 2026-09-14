package com.job_Portal_Backend.job_portal_backend.reports.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportRequest {

    @NotBlank(message = "Target type is required")
    private String targetType;

    @NotNull(message = "Target id is required")
    private Long targetId;

    @NotBlank(message = "Report type is required")
    private String reportType;

    private String description;
}
