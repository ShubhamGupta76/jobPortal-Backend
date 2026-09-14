package com.job_Portal_Backend.job_portal_backend.messaging.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {
    @NotNull(message = "Application ID is required")
    private Long applicationId;
}