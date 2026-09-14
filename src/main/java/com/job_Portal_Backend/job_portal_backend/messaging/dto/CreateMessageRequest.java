package com.job_Portal_Backend.job_portal_backend.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMessageRequest {
    @NotBlank(message = "Message content is required")
    private String content;
    private String messageType = "TEXT";
}