package com.job_Portal_Backend.job_portal_backend.messaging.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private String messageType;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private boolean read;
}