package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardActivityItemDto {
    private String type;
    private String title;
    private String subtitle;
    private LocalDateTime timestamp;
}
