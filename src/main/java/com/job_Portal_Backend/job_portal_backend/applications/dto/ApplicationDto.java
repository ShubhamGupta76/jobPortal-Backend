package com.job_Portal_Backend.job_portal_backend.applications.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationDto {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String userLocation;
    private String userHeadline;
    private Long jobId;
    private String jobTitle;
    private String status;
    private Long assessmentId;
    private String assessmentTitle;
    private String resumePath;
    private String resumeDownloadUrl;
    private String coverLetter;
    private String source;
    private Long assessmentSessionId;
    private Double assessmentScore;
    private Boolean assessmentPassed;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private String assessmentAnalysis;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
