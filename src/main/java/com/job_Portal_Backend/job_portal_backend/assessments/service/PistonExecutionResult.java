package com.job_Portal_Backend.job_portal_backend.assessments.service;

import lombok.Data;

@Data
public class PistonExecutionResult {
    private String stdout;
    private String stderr;
    private String output;
    private Integer code;
    private String status;
    private String message;
    private Long cpuTimeMs;
    private Long wallTimeMs;
    private Long memoryBytes;
}
