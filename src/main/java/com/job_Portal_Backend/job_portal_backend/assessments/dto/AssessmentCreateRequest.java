package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.Data;

@Data
public class AssessmentCreateRequest {
    private String title;
    private String description;
    private Long jobId;
    private Integer durationMinutes;
    private Integer totalMarks;
    private String passingMarksPercentage;
    private Boolean shuffleQuestions;
    private Boolean allowBackNavigation;
    private Boolean enableProctoring;
    private Boolean detectCopyPaste;
    private Boolean enforceFullScreen;
    private Boolean requireWebcam;
    private Boolean desktopOnly;
    private Boolean sequentialQuestionsOnly;
    private Boolean lockAnsweredQuestions;
    private Boolean autoSubmitOnViolationLimit;
    private Integer fullscreenViolationLimit;
    private Integer tabSwitchLimit;
    private Integer offlineGraceSeconds;
    private Integer maxAttempts;
}
