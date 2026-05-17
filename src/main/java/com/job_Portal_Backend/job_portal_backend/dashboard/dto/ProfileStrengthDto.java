package com.job_Portal_Backend.job_portal_backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileStrengthDto {
    private int completionPercentage;
    private String level;
    private String message;
}
