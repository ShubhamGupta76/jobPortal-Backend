package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResultDto {
    private Integer index;
    private String input;
    private String expectedOutput;
    private String actualOutput;
    private String stderr;
    private Boolean passed;
    private Integer exitCode;
    private String status;
    private Long runtimeMs;
    private Long memoryBytes;
}
