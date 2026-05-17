package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment.AssessmentStatus;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentDto {
    private Long id;
    private String title;
    private String description;
    private Long recruiterId;
    private String recruiterName;
    private Long jobId;
    private String jobTitle;
    private Integer durationMinutes;
    private Integer totalMarks;
    private String passingMarksPercentage;
    private AssessmentStatus status;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<QuestionDto> questions;
    private Long questionCount;

    // Static mapping methods
    public static AssessmentDto fromAssessment(
            com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment assessment,
            List<QuestionDto> questionDtos) {
        if (assessment == null)
            return null;
        AssessmentDto dto = new AssessmentDto();
        dto.setId(assessment.getId());
        dto.setTitle(assessment.getTitle());
        dto.setDescription(assessment.getDescription());
        if (assessment.getRecruiter() != null) {
            dto.setRecruiterId(assessment.getRecruiter().getId());
            dto.setRecruiterName(
                    (assessment.getRecruiter().getFirstName() != null ? assessment.getRecruiter().getFirstName() : "")
                            + " "
                            + (assessment.getRecruiter().getLastName() != null ? assessment.getRecruiter().getLastName()
                                    : ""));
        }
        if (assessment.getJob() != null) {
            dto.setJobId(assessment.getJob().getId());
            dto.setJobTitle(assessment.getJob().getTitle());
        }
        dto.setDurationMinutes(assessment.getDurationMinutes());
        dto.setTotalMarks(assessment.getTotalMarks());
        dto.setPassingMarksPercentage(assessment.getPassingMarksPercentage());
        dto.setStatus(assessment.getStatus());
        dto.setShuffleQuestions(assessment.getShuffleQuestions());
        dto.setAllowBackNavigation(assessment.getAllowBackNavigation());
        dto.setEnableProctoring(assessment.getEnableProctoring());
        dto.setDetectCopyPaste(assessment.getDetectCopyPaste());
        dto.setEnforceFullScreen(assessment.getEnforceFullScreen());
        dto.setRequireWebcam(assessment.getRequireWebcam());
        dto.setDesktopOnly(assessment.getDesktopOnly());
        dto.setSequentialQuestionsOnly(assessment.getSequentialQuestionsOnly());
        dto.setLockAnsweredQuestions(assessment.getLockAnsweredQuestions());
        dto.setAutoSubmitOnViolationLimit(assessment.getAutoSubmitOnViolationLimit());
        dto.setFullscreenViolationLimit(assessment.getFullscreenViolationLimit());
        dto.setTabSwitchLimit(assessment.getTabSwitchLimit());
        dto.setOfflineGraceSeconds(assessment.getOfflineGraceSeconds());
        dto.setMaxAttempts(assessment.getMaxAttempts());
        dto.setCreatedAt(assessment.getCreatedAt());
        dto.setUpdatedAt(assessment.getUpdatedAt());
        dto.setQuestions(questionDtos);
        dto.setQuestionCount(questionDtos != null ? (long) questionDtos.size() : 0L);
        return dto;
    }
}
