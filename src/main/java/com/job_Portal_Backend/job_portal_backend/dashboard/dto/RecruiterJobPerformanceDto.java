package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterJobPerformanceDto {
    private Long jobId;
    private String title;
    private String category;
    private long applicants;
    private String status;
}
