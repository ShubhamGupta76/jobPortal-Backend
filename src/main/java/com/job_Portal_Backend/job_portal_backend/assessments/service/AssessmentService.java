package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentCreateRequest;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentSummaryDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.QuestionDto;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment.AssessmentStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Application.ApplicationStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.*;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
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
    private ApplicationRepository applicationRepository;

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
        assessment.setShuffleQuestions(Boolean.TRUE.equals(dto.getShuffleQuestions()));
        assessment.setAllowBackNavigation(Boolean.TRUE.equals(dto.getAllowBackNavigation()));
        assessment.setEnableProctoring(Boolean.TRUE.equals(dto.getEnableProctoring()));
        assessment.setDetectCopyPaste(Boolean.TRUE.equals(dto.getDetectCopyPaste()));
        assessment.setEnforceFullScreen(Boolean.TRUE.equals(dto.getEnforceFullScreen()));
        applySecuritySettings(assessment, dto);
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
        existing.setRequireWebcam(updatedAssessment.getRequireWebcam());
        existing.setDesktopOnly(updatedAssessment.getDesktopOnly());
        existing.setSequentialQuestionsOnly(updatedAssessment.getSequentialQuestionsOnly());
        existing.setLockAnsweredQuestions(updatedAssessment.getLockAnsweredQuestions());
        existing.setAutoSubmitOnViolationLimit(updatedAssessment.getAutoSubmitOnViolationLimit());
        existing.setFullscreenViolationLimit(updatedAssessment.getFullscreenViolationLimit());
        existing.setTabSwitchLimit(updatedAssessment.getTabSwitchLimit());
        existing.setOfflineGraceSeconds(updatedAssessment.getOfflineGraceSeconds());
        existing.setMaxAttempts(updatedAssessment.getMaxAttempts());
        existing.setUpdatedAt(LocalDateTime.now());
        return assessmentRepository.save(existing);
    }

    public Assessment updateAssessment(Long assessmentId, AssessmentCreateRequest dto) {
        Assessment existing = getAssessmentEntity(assessmentId);
        if (dto.getJobId() != null && (existing.getJob() == null || !dto.getJobId().equals(existing.getJob().getId()))) {
            Job job = jobRepository.findById(dto.getJobId())
                    .orElseThrow(() -> new RuntimeException("Job not found: " + dto.getJobId()));
            existing.setJob(job);
        }

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setDurationMinutes(dto.getDurationMinutes());
        existing.setTotalMarks(dto.getTotalMarks());
        existing.setPassingMarksPercentage(dto.getPassingMarksPercentage());
        existing.setShuffleQuestions(Boolean.TRUE.equals(dto.getShuffleQuestions()));
        existing.setAllowBackNavigation(Boolean.TRUE.equals(dto.getAllowBackNavigation()));
        existing.setEnableProctoring(Boolean.TRUE.equals(dto.getEnableProctoring()));
        existing.setDetectCopyPaste(Boolean.TRUE.equals(dto.getDetectCopyPaste()));
        existing.setEnforceFullScreen(Boolean.TRUE.equals(dto.getEnforceFullScreen()));
        applySecuritySettings(existing, dto);
        existing.setUpdatedAt(LocalDateTime.now());
        return assessmentRepository.save(existing);
    }

    private void applySecuritySettings(Assessment assessment, AssessmentCreateRequest dto) {
        assessment.setRequireWebcam(Boolean.TRUE.equals(dto.getRequireWebcam()));
        assessment.setDesktopOnly(dto.getDesktopOnly() == null || Boolean.TRUE.equals(dto.getDesktopOnly()));
        assessment.setSequentialQuestionsOnly(Boolean.TRUE.equals(dto.getSequentialQuestionsOnly()));
        assessment.setLockAnsweredQuestions(Boolean.TRUE.equals(dto.getLockAnsweredQuestions()));
        assessment.setAutoSubmitOnViolationLimit(dto.getAutoSubmitOnViolationLimit() == null || Boolean.TRUE.equals(dto.getAutoSubmitOnViolationLimit()));
        assessment.setFullscreenViolationLimit(dto.getFullscreenViolationLimit() != null ? dto.getFullscreenViolationLimit() : 3);
        assessment.setTabSwitchLimit(dto.getTabSwitchLimit() != null ? dto.getTabSwitchLimit() : 3);
        assessment.setOfflineGraceSeconds(dto.getOfflineGraceSeconds() != null ? dto.getOfflineGraceSeconds() : 60);
        assessment.setMaxAttempts(dto.getMaxAttempts() != null ? dto.getMaxAttempts() : 1);
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

        List<Application> assignedApplications = applicationRepository.findByAssignedAssessmentId(assessmentId);
        for (Application application : assignedApplications) {
            application.setAssignedAssessment(null);
            if (application.getStatus() == ApplicationStatus.ASSESSMENT) {
                application.setStatus(ApplicationStatus.SHORTLISTED);
            }
        }
        applicationRepository.saveAll(assignedApplications);

        List<TestSession> sessions = testSessionRepository.findByAssessmentId(assessmentId);
        List<Long> sessionIds = sessions.stream()
                .map(TestSession::getId)
                .toList();

        if (!sessionIds.isEmpty()) {
            proctoringLogRepository.deleteByTestSessionIdIn(sessionIds);
            resultRepository.deleteByTestSessionIdIn(sessionIds);
            submissionRepository.deleteByTestSessionIdIn(sessionIds);
            testSessionRepository.deleteByAssessmentId(assessmentId);
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
        existing.setType(updatedQuestion.getType());
        existing.setMarks(updatedQuestion.getMarks());
        existing.setSequenceNumber(updatedQuestion.getSequenceNumber() != null ? updatedQuestion.getSequenceNumber() : existing.getSequenceNumber());
        existing.setDifficulty(updatedQuestion.getDifficulty());
        existing.setOption1(updatedQuestion.getOption1());
        existing.setOption2(updatedQuestion.getOption2());
        existing.setOption3(updatedQuestion.getOption3());
        existing.setOption4(updatedQuestion.getOption4());
        existing.setCorrectAnswer(updatedQuestion.getCorrectAnswer());
        existing.setExplanation(updatedQuestion.getExplanation());
        existing.setCodeTemplate(updatedQuestion.getCodeTemplate());
        existing.setProgrammingLanguage(updatedQuestion.getProgrammingLanguage());
        existing.setTestCases(updatedQuestion.getTestCases());
        existing.setExpectedOutput(updatedQuestion.getExpectedOutput());
        existing.setSampleTestCases(updatedQuestion.getSampleTestCases());
        existing.setHiddenTestCases(updatedQuestion.getHiddenTestCases());
        existing.setFunctionSignature(updatedQuestion.getFunctionSignature());
        existing.setHiddenWrapperCode(updatedQuestion.getHiddenWrapperCode());
        existing.setConstraintsText(updatedQuestion.getConstraintsText());
        existing.setUpdatedAt(LocalDateTime.now());
        return questionRepository.save(existing);
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    public Question getQuestionEntity(Long questionId) {
        return questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
    }
}
