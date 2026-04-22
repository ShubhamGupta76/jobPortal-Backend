package com.job_Portal_Backend.job_portal_backend.assessments.controller;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.*;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.service.*;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.QuestionRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/submissions")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private TestSessionService testSessionService;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * POST /api/v1/submissions
     * Candidate submits answer to a question (auto-save as they answer)
     */
    @PostMapping
    public ResponseEntity<SubmissionDto> submitAnswer(
            @Valid @RequestBody SubmitAnswerRequest request) {
        String sessionToken = request.getSessionToken();
        Long questionId = request.getQuestionId();

        log.info("Submit answer request: session={}, question={}", sessionToken, questionId);

        try {
            TestSession session = testSessionService.getSessionByToken(sessionToken);

            // Validate session is still active
            if (!testSessionService.isSessionValid(sessionToken)) {
                log.warn("Invalid session token: {}", sessionToken);
                return ResponseEntity.status(401).body(null);
            }

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found: " + questionId));

            // Create submission
            Submission submission = new Submission();
            submission.setTestSession(session);
            submission.setQuestion(question);
            submission.setAnswerText(request.getAnswerText());
            submission.setCodeSubmitted(request.getCodeSubmitted());

            // Auto-evaluate based on question type
            Submission saved = evaluationService.submitAnswer(submission);

            if (question.getType() != null) {
                switch (question.getType().name()) {
                    case "MCQ":
                        saved = evaluationService.evaluateMCQQuestion(saved);
                        break;
                    case "DESCRIPTIVE":
                        saved = evaluationService.storeDescriptiveAnswer(saved);
                        break;
                    // CODING handled separately
                }
            }

            log.info("Answer saved successfully for submission ID: {}", saved.getId());

            // DTO to avoid Hibernate proxy serialization error
            SubmissionDto submissionDto = new SubmissionDto();
            submissionDto.setId(saved.getId());
            submissionDto.setAnswerText(saved.getAnswerText());
            submissionDto.setMarksObtained(saved.getMarksObtained());
            submissionDto.setIsCorrect(saved.getIsCorrect());
            submissionDto.setEvaluationDetails(saved.getEvaluationDetails());
            return ResponseEntity.ok(submissionDto);
        } catch (RuntimeException e) {
            log.error("Error processing answer submission: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle
        }
    }

    /**
     * POST /api/v1/submissions/{submissionId}/evaluate-coding
     * Backend evaluates coding submission after execution
     * (Frontend sends code, backend runs it via Judge0, stores result)
     */
    @PostMapping("/{submissionId}/evaluate-coding")
    public ResponseEntity<Submission> evaluateCodingSubmission(
            @PathVariable Long submissionId,
            @RequestBody CodeExecutionRequest request) {

        // TODO: Call Judge0 API or custom code executor
        String executionOutput = executeCode(request.getCode(), request.getLanguage(), request.getInput());

        Submission submission = evaluationService.getSubmissionById(submissionId);
        Submission evaluated = evaluationService.evaluateCodingQuestion(submission, executionOutput);

        return ResponseEntity.ok(evaluated);
    }

    /**
     * GET /api/v1/submissions/{sessionId}
     * Get all submissions for a session (for review/progress tracking)
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<List<Submission>> getSessionSubmissions(@PathVariable Long sessionId) {
        List<Submission> submissions = evaluationService.getSessionSubmissions(sessionId);
        return ResponseEntity.ok(submissions);
    }

    private String executeCode(String code, String language, String input) {
        // TODO: Implement code execution via Judge0 or custom runner
        // For now, return placeholder
        return "Output: Code execution not yet implemented";
    }
}
