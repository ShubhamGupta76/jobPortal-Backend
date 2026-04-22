package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private String candidateName;
    private Double score;
    private Double percentageScore;
    private Integer rank;
    private Boolean passed;
    private String completedAt;
}
