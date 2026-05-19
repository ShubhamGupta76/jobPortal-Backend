package com.job_Portal_Backend.job_portal_backend.interview.dto;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewMessageType;

import java.time.LocalDateTime;

public record InterviewMessageDto(
        Long id,
        Long senderId,
        String senderName,
        InterviewMessageType type,
        String content,
        LocalDateTime createdAt
) {
}
