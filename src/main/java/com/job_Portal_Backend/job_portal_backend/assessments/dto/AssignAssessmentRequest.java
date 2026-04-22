package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignAssessmentRequest {
    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    private Long assessmentId;
}
