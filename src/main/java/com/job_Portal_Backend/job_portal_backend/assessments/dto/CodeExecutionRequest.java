package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionRequest {
    private Long questionId;
    private String sessionToken;
    private String language;
    private String code;
    private String input; // for test cases
    private Boolean runHiddenTests;
}

