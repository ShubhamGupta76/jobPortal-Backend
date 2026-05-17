package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment.AssessmentStatus;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSummaryDto {
    private Long id;
    private String title;
    private String description;
    private UserDto recruiter;
    private Long jobId;
    private String jobTitle;
    private Integer durationMinutes;
    private Integer totalMarks;
    private String passingMarksPercentage;
    private AssessmentStatus status;
    private Boolean shuffleQuestions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long questionCount;

    public static AssessmentSummaryDto fromAssessment(
            com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment assessment) {
        if (assessment == null)
            return null;
        AssessmentSummaryDto dto = new AssessmentSummaryDto();
        dto.setId(assessment.getId());
        dto.setTitle(assessment.getTitle());
        dto.setDescription(assessment.getDescription());
        dto.setRecruiter(UserDto.fromUser(assessment.getRecruiter()));
        dto.setJobId(assessment.getJob() != null ? assessment.getJob().getId() : null);
        dto.setJobTitle(assessment.getJob() != null ? assessment.getJob().getTitle() : null);
        dto.setDurationMinutes(assessment.getDurationMinutes());
        dto.setTotalMarks(assessment.getTotalMarks());
        dto.setPassingMarksPercentage(assessment.getPassingMarksPercentage());
        dto.setStatus(assessment.getStatus());
        dto.setShuffleQuestions(assessment.getShuffleQuestions());
        dto.setCreatedAt(assessment.getCreatedAt());
        dto.setUpdatedAt(assessment.getUpdatedAt());
        // Use safe question count - avoid lazy loading
        List<com.job_Portal_Backend.job_portal_backend.assessments.entity.Question> questions = assessment
                .getQuestions();
        dto.setQuestionCount(questions != null ? (long) questions.size() : 0L);
        return dto;
    }
}
