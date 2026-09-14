package com.job_Portal_Backend.job_portal_backend.applications.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationTimelineEventDto {
    private Long id;
    private String previousStatus;
    private String status;
    private String actorName;
    private LocalDateTime timestamp;
    private String note;
}