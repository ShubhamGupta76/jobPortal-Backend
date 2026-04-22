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
public class NotificationDto {
    private Long id;
    private Long userId;
    private String type; // JOB_APPLICATION, INTERVIEW, SYSTEM, etc.
    private String title;
    private String message;
    private String data; // JSON string for additional data
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}