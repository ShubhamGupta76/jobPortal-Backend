package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeExecutionResponse {
    private Boolean success;
    private String output;
    private String error;
    private Long executionTime;
    private Long memoryUsed;
    private Integer passedCount;
    private Integer totalCount;
    private Double scorePercentage;
    private List<TestCaseResultDto> testCases = new ArrayList<>();
}
