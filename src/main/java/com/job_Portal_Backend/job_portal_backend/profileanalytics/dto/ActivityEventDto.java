package com.job_Portal_Backend.job_portal_backend.profileanalytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityEventDto {
    private String type;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private String entityType;
    private Long entityId;
}
