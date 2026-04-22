package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentCreateRequest;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentSummaryDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.QuestionDto;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment.AssessmentStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.*;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentService {

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private ProctoringLogRepository proctoringLogRepository;

    public Assessment createAssessment(AssessmentCreateRequest dto, User recruiter) {
        Job job = jobRepository.findById(dto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found: " + dto.getJobId()));

        Assessment assessment = new Assessment();
        assessment.setTitle(dto.getTitle());
        assessment.setDescription(dto.getDescription());
        assessment.setJob(job);
        assessment.setRecruiter(recruiter);
        assessment.setDurationMinutes(dto.getDurationMinutes());
        assessment.setTotalMarks(dto.getTotalMarks());
        assessment.setPassingMarksPercentage(dto.getPassingMarksPercentage());
        assessment.setStatus(AssessmentStatus.DRAFT);
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());
        return assessmentRepository.save(assessment);
    }

    public List<AssessmentSummaryDto> getRecruiterAssessments(Long recruiterId) {
        List<Assessment> assessments = assessmentRepository.findByRecruiterId(recruiterId);
        return assessments.stream()
                .map(AssessmentSummaryDto::fromAssessment)
                .collect(Collectors.toList());
    }

    public AssessmentDto getAssessmentDetails(Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
        List<Question> questions = questionRepository.findByAssessmentIdOrderBySequenceNumber(assessmentId);
        List<QuestionDto> questionDtos = questions.stream()
                .map(QuestionDto::fromQuestion)
                .collect(Collectors.toList());
        return AssessmentDto.fromAssessment(assessment, questionDtos);
    }

    public Assessment getAssessmentEntity(Long assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));
    }

    public Assessment updateAssessment(Long assessmentId, Assessment updatedAssessment) {
        Assessment existing = getAssessmentEntity(assessmentId);
        existing.setTitle(updatedAssessment.getTitle());
        existing.setDescription(updatedAssessment.getDescription());
        existing.setDurationMinutes(updatedAssessment.getDurationMinutes());
        existing.setTotalMarks(updatedAssessment.getTotalMarks());
        existing.setPassingMarksPercentage(updatedAssessment.getPassingMarksPercentage());
        existing.setShuffleQuestions(updatedAssessment.getShuffleQuestions());
        existing.setAllowBackNavigation(updatedAssessment.getAllowBackNavigation());
        existing.setEnableProctoring(updatedAssessment.getEnableProctoring());
        existing.setDetectCopyPaste(updatedAssessment.getDetectCopyPaste());
        existing.setEnforceFullScreen(updatedAssessment.getEnforceFullScreen());
        existing.setUpdatedAt(LocalDateTime.now());
        return assessmentRepository.save(existing);
    }

    public Assessment publishAssessment(Long assessmentId) {
        Assessment assessment = getAssessmentEntity(assessmentId);

        List<Question> questions = questionRepository.findByAssessmentId(assessmentId);
        if (questions.isEmpty()) {
            throw new RuntimeException("Cannot publish assessment without questions");
        }

        assessment.setStatus(AssessmentStatus.PUBLISHED);
        assessment.setUpdatedAt(LocalDateTime.now());
        return assessmentRepository.save(assessment);
    }

    public Assessment closeAssessment(Long assessmentId) {
        Assessment assessment = getAssessmentEntity(assessmentId);
        assessment.setStatus(AssessmentStatus.CLOSED);
        assessment.setUpdatedAt(LocalDateTime.now());
        return assessmentRepository.save(assessment);
    }

    public void deleteAssessment(Long assessmentId) {
        Assessment assessment = getAssessmentEntity(assessmentId);
        if (!assessment.getStatus().equals(AssessmentStatus.DRAFT)) {
            throw new RuntimeException("Can only delete DRAFT assessments");
        }
        assessmentRepository.deleteById(assessmentId);
    }

    public List<AssessmentSummaryDto> getAvailableAssessmentsForJob(Long jobId) {
        List<Assessment> assessments = assessmentRepository.findByJobId(jobId);
        return assessments.stream()
                .filter(a -> a.getStatus().equals(AssessmentStatus.PUBLISHED))
                .map(AssessmentSummaryDto::fromAssessment)
                .collect(Collectors.toList());
    }

    public Question addQuestion(Question question) {
        List<Question> existing = questionRepository
                .findByAssessmentIdOrderBySequenceNumber(question.getAssessment().getId());
        int nextSeq = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getSequenceNumber() + 1;
        question.setSequenceNumber(nextSeq);
        question.setCreatedAt(LocalDateTime.now());
        question.setUpdatedAt(LocalDateTime.now());
        return questionRepository.save(question);
    }

    public List<Question> getAssessmentQuestions(Long assessmentId) {
        return questionRepository.findByAssessmentIdOrderBySequenceNumber(assessmentId);
    }

    public Question updateQuestion(Long questionId, Question updatedQuestion) {
        Question existing = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        if (existing.getAssessment().getId() != updatedQuestion.getAssessment().getId()) {
            throw new RuntimeException("Cannot change assessment for question");
        }
        existing.setTitle(updatedQuestion.getTitle());
        existing.setDescription(updatedQuestion.getDescription());
        existing.setMarks(updatedQuestion.getMarks());
        existing.setDifficulty(updatedQuestion.getDifficulty());
        existing.setUpdatedAt(LocalDateTime.now());
        return questionRepository.save(existing);
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }
}
