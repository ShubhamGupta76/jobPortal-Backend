package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProctoringEventRequest {
    private String sessionToken;
    private String eventType;
    private String description;
    private Integer severityScore;
    private String metadata;
}
