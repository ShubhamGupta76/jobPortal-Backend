package com.job_Portal_Backend.job_portal_backend.applications.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;
}
