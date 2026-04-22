package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionRequest {
    private String language;
    private String code;
    private String input; // for test cases
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CodeExecutionResponse {
    private Boolean success;
    private String output;
    private String error;
    private Long executionTime; // milliseconds
    private Double memoryUsed; // MB
}
