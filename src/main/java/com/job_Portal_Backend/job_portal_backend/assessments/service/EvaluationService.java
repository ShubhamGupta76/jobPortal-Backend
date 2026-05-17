package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.*;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.CodeExecutionRequest;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.CodeExecutionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EvaluationService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private CodingExecutionService codingExecutionService;

    /**
     * Store answer submission for a question
     */
    public Submission submitAnswer(Submission submission) {
        Optional<Submission> existing = submissionRepository.findByTestSessionIdAndQuestionId(
                submission.getTestSession().getId(),
                submission.getQuestion().getId());

        if (existing.isPresent()) {
            Submission saved = existing.get();
            saved.setAnswerText(submission.getAnswerText());
            saved.setCodeSubmitted(submission.getCodeSubmitted());
            saved.setSubmittedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            return submissionRepository.save(saved);
        }

        submission.setCreatedAt(LocalDateTime.now());
        submission.setUpdatedAt(LocalDateTime.now());
        submission.setSubmittedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }

    /**
     * Auto-evaluate MCQ answer: compare with correct option
     */
    public Submission evaluateMCQQuestion(Submission submission) {
        Question question = submission.getQuestion();
        String correctAnswer = question.getCorrectAnswer();
        Boolean isCorrect = correctAnswer.equals(submission.getAnswerText());

        submission.setIsCorrect(isCorrect);
        submission.setEvaluatedAt(LocalDateTime.now());

        if (isCorrect) {
            submission.setMarksObtained(Double.valueOf(question.getMarks()));
        } else {
            submission.setMarksObtained(0.0);
        }

        submission.setEvaluationDetails("MCQ Question - " + (isCorrect ? "CORRECT" : "INCORRECT"));
        submission.setUpdatedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    /**
     * Evaluate coding question: compare output with expected output
     * (In production, use Judge0 API for execution)
     */
    public Submission evaluateCodingQuestion(Submission submission, String executionOutput) {
        Question question = submission.getQuestion();
        String expectedOutput = question.getExpectedOutput();

        // Normalize outputs for comparison
        String normalizedOutput = executionOutput.trim().replaceAll("\\s+", " ");
        String normalizedExpected = expectedOutput.trim().replaceAll("\\s+", " ");

        Boolean isCorrect = normalizedOutput.equals(normalizedExpected);

        submission.setIsCorrect(isCorrect);
        submission.setEvaluatedAt(LocalDateTime.now());

        if (isCorrect) {
            submission.setMarksObtained(Double.valueOf(question.getMarks()));
        } else {
            submission.setMarksObtained(0.0);
        }

        submission.setEvaluationDetails("Coding Question - Output: " + executionOutput);
        submission.setUpdatedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    public Submission evaluateCodingSubmission(Submission submission, String language) {
        Question question = submission.getQuestion();
        CodeExecutionRequest request = new CodeExecutionRequest();
        request.setQuestionId(question.getId());
        request.setLanguage(language != null
                ? language
                : question.getProgrammingLanguage() != null ? question.getProgrammingLanguage().name() : "PYTHON");
        request.setCode(submission.getCodeSubmitted());
        request.setRunHiddenTests(true);

        CodeExecutionResponse execution = codingExecutionService.execute(request);
        boolean isCorrect = Boolean.TRUE.equals(execution.getSuccess());
        double scoreRatio = execution.getScorePercentage() == null ? 0.0 : execution.getScorePercentage() / 100.0;

        submission.setIsCorrect(isCorrect);
        submission.setMarksObtained(question.getMarks() * scoreRatio);
        submission.setEvaluatedAt(LocalDateTime.now());
        submission.setEvaluationDetails(String.format(
                "Coding Question - %d/%d hidden tests passed | runtime=%sms | memory=%s bytes",
                execution.getPassedCount(),
                execution.getTotalCount(),
                execution.getExecutionTime(),
                execution.getMemoryUsed()));
        submission.setUpdatedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }

    public void evaluateCodingSubmissionsForSession(Long testSessionId) {
        List<Submission> submissions = submissionRepository.findByTestSessionId(testSessionId);
        for (Submission submission : submissions) {
            Question question = submission.getQuestion();
            if (question.getType() == Question.QuestionType.CODING && submission.getCodeSubmitted() != null) {
                evaluateCodingSubmission(submission,
                        question.getProgrammingLanguage() != null ? question.getProgrammingLanguage().name() : null);
            }
        }
    }

    /**
     * Descriptive questions are marked by recruiter manually
     * Store submission without auto-evaluation
     */
    public Submission storeDescriptiveAnswer(Submission submission) {
        submission.setEvaluationDetails("Pending manual evaluation by recruiter");
        submission.setUpdatedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }

    /**
     * Get all submissions for a test session
     */
    public List<Submission> getSessionSubmissions(Long testSessionId) {
        return submissionRepository.findByTestSessionId(testSessionId);
    }

    /**
     * Get submission by ID
     */
    public Submission getSubmissionById(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
    }

    /**
     * Auto-generate Result after test submission
     * Aggregates scores, calculates percentage, determines pass/fail
     */
    public Result evaluateTestSession(Long testSessionId) {
        TestSession testSession = testSessionRepository.findById(testSessionId)
                .orElseThrow(() -> new RuntimeException("Test session not found"));

        evaluateCodingSubmissionsForSession(testSessionId);
        List<Submission> submissions = submissionRepository.findByTestSessionId(testSessionId);
        List<Question> questions = questionRepository.findByAssessmentId(
                testSession.getAssessment().getId());

        // Calculate total marks
        Double totalMarksObtained = submissions.stream()
                .filter(s -> s.getMarksObtained() != null)
                .mapToDouble(Submission::getMarksObtained)
                .sum();

        Integer maxMarks = questions.stream()
                .mapToInt(Question::getMarks)
                .sum();

        Double percentageScore = maxMarks > 0 ? (totalMarksObtained / maxMarks) * 100 : 0.0;

        // Determine pass/fail
        Double passingPercentage = Double.parseDouble(
                testSession.getAssessment().getPassingMarksPercentage().replace("%", ""));
        Boolean passed = percentageScore >= passingPercentage;

        // Count correct answers
        Long correctCount = submissionRepository.countByTestSessionIdAndIsCorrectTrue(testSessionId);

        // Check for existing result
        Optional<Result> existingResult = resultRepository.findByTestSessionId(testSessionId);
        Result result;

        if (existingResult.isPresent()) {
            result = existingResult.get();
        } else {
            result = new Result();
            result.setTestSession(testSession);
            result.setCreatedAt(LocalDateTime.now());
        }

        result.setTotalMarksObtained(totalMarksObtained);
        result.setMaxMarks(Double.valueOf(maxMarks));
        result.setPercentageScore(percentageScore);
        result.setPassed(passed);
        result.setCorrectAnswers(Math.toIntExact(correctCount));
        result.setTotalQuestions(questions.size());
        result.setDetailedAnalysis(generateAnalysis(percentageScore, passed, correctCount, questions.size()));
        result.setUpdatedAt(LocalDateTime.now());

        // Update test session status to EVALUATED
        testSession.setStatus(TestSession.SessionStatus.EVALUATED);
        testSessionRepository.save(testSession);

        return resultRepository.save(result);
    }

    /**
     * Get result for test session
     */
    public Result getTestResult(Long testSessionId) {
        return resultRepository.findByTestSessionId(testSessionId)
                .orElseThrow(() -> new RuntimeException("Result not found"));
    }

    /**
     * Get leaderboard for assessment (all candidates ranked by score)
     */
    public List<Result> getAssessmentLeaderboard(Long assessmentId) {
        return resultRepository.findByTestSessionAssessmentIdOrderByPercentageScoreDesc(assessmentId);
    }

    /**
     * Generate detailed analysis string for result
     */
    private String generateAnalysis(Double percentageScore, Boolean passed, Long correctAnswers,
            Integer totalQuestions) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("Score: ").append(String.format("%.2f", percentageScore)).append("% | ");
        analysis.append("Correct: ").append(correctAnswers).append("/").append(totalQuestions).append(" | ");
        analysis.append("Status: ").append(passed ? "PASSED" : "FAILED");
        return analysis.toString();
    }
}
