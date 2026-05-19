package com.job_Portal_Backend.job_portal_backend.interview.dto;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewMessageType;
import jakarta.validation.constraints.NotBlank;

public record InterviewMessageRequest(
        InterviewMessageType type,
        @NotBlank String content
) {
}
