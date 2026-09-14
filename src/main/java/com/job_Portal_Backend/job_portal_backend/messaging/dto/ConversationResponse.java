package com.job_Portal_Backend.job_portal_backend.messaging.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationResponse {
    private Long id;
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long otherUserId;
    private String otherUserName;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;
    private LocalDateTime updatedAt;
}