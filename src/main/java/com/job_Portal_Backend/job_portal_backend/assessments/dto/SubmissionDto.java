package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDto {
    private Long id;
    private String answerText;
    private Double marksObtained;
    private Boolean isCorrect;
    private String evaluationDetails;
}
