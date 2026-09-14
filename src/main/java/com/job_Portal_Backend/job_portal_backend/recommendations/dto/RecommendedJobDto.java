package com.job_Portal_Backend.job_portal_backend.recommendations.dto;

import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import lombok.Data;

import java.util.List;

@Data
public class RecommendedJobDto {
    /** The underlying job, including JobMatchingService's own base match fields (matchScore, matchingSkills, etc.). */
    private JobDto job;

    /** The recommendation engine's ranking score: JobMatchingService's base score plus bounded contextual boosts. */
    private int finalScore;

    /** Deterministic reason codes explaining why this job was recommended (see RecommendationReason). */
    private List<String> reasonCodes;
}
