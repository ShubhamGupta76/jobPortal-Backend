package com.job_Portal_Backend.job_portal_backend.assessments.controller;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.QuestionRepository;
import com.job_Portal_Backend.job_portal_backend.assessments.service.*;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Submission;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/api/v1/test-sessions")
public class TestSessionController {

    @Autowired
    private TestSessionService testSessionService;

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private QuestionRepository questionRepository;

    /**
     * POST /api/v1/test-sessions/start
     * Candidate starts a test. Returns session token for subsequent requests.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startTestSession(
            @RequestBody Map<String, Object> req,
            Authentication auth,
            HttpServletRequest request) {

        Long assessmentId = Long.valueOf(String.valueOf(req.get("assessmentId")));
        String deviceFingerprint = req.get("deviceFingerprint") != null ? String.valueOf(req.get("deviceFingerprint")) : "";
        User user = (User) auth.getPrincipal();
        System.out.println("Assessment ID: " + assessmentId);
        System.out.println("User ID: " + user.getId());

        String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "";
        String ipAddress = getClientIp(request);

        TestSession session = testSessionService.startTestSession(
                assessmentId, user.getId(), userAgent, ipAddress, deviceFingerprint);

        // Mark session as started
        TestSession activeSession = testSessionService.markSessionStarted(session.getSessionToken());

        // Return session info to frontend
        Map<String, Object> response = new HashMap<>();
        response.put("sessionToken", activeSession.getSessionToken());
        response.put("sessionId", activeSession.getId());
        response.put("durationMinutes", activeSession.getAssessment().getDurationMinutes());
        response.put("expiresAt", activeSession.getExpiresAt());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/test-sessions/info
     * Get current session info (used to validate session on page load)
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSessionInfo(
            @RequestParam String sessionToken) {

        if (!testSessionService.isSessionValid(sessionToken)) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Session expired or invalid"));
        }

        TestSession session = testSessionService.getSessionByToken(sessionToken);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("assessmentId", session.getAssessment().getId());
        response.put("assessmentTitle", session.getAssessment().getTitle());
        response.put("assessmentDescription", session.getAssessment().getDescription());
        response.put("jobTitle", session.getAssessment().getJob() != null ? session.getAssessment().getJob().getTitle() : null);
        response.put("durationMinutes", session.getAssessment().getDurationMinutes());
        response.put("totalMarks", session.getAssessment().getTotalMarks());
        response.put("status", session.getStatus().toString());
        response.put("expiresAt", session.getExpiresAt());
        response.put("allowBackNavigation", session.getAssessment().getAllowBackNavigation());
        response.put("enableProctoring", session.getAssessment().getEnableProctoring());
        response.put("detectCopyPaste", session.getAssessment().getDetectCopyPaste());
        response.put("enforceFullScreen", session.getAssessment().getEnforceFullScreen());
        response.put("requireWebcam", session.getAssessment().getRequireWebcam());
        response.put("desktopOnly", session.getAssessment().getDesktopOnly());
        response.put("sequentialQuestionsOnly", session.getAssessment().getSequentialQuestionsOnly());
        response.put("lockAnsweredQuestions", session.getAssessment().getLockAnsweredQuestions());
        response.put("autoSubmitOnViolationLimit", session.getAssessment().getAutoSubmitOnViolationLimit());
        response.put("fullscreenViolationLimit", session.getAssessment().getFullscreenViolationLimit());
        response.put("tabSwitchLimit", session.getAssessment().getTabSwitchLimit());
        response.put("offlineGraceSeconds", session.getAssessment().getOfflineGraceSeconds());

        List<Question> questions = questionRepository.findByAssessmentIdOrderBySequenceNumber(session.getAssessment().getId());
        if (Boolean.TRUE.equals(session.getAssessment().getShuffleQuestions())) {
            Collections.shuffle(questions, new Random(session.getId()));
        }
        List<Map<String, Object>> sanitizedQuestions = questions.stream()
                .map(this::toCandidateQuestion)
                .toList();
        response.put("questions", sanitizedQuestions);

        List<Submission> submissions = evaluationService.getSessionSubmissions(session.getId());
        Map<Long, String> savedAnswers = new HashMap<>();
        submissions.forEach(submission -> savedAnswers.put(
                submission.getQuestion().getId(),
                submission.getQuestion().getType() == Question.QuestionType.CODING
                        ? (submission.getCodeSubmitted() != null ? submission.getCodeSubmitted() : "")
                        : (submission.getAnswerText() != null ? submission.getAnswerText() : "")));
        response.put("answers", savedAnswers);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/test-sessions/submit
     * Candidate submits entire test (triggers evaluation)
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTest(
            @RequestParam String sessionToken) {

        TestSession submitted = testSessionService.submitTest(sessionToken);

        // Auto-evaluate test
        Result result = evaluationService.evaluateTestSession(submitted.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", submitted.getId());
        response.put("totalScore", result.getTotalMarksObtained());
        response.put("maxScore", result.getMaxMarks());
        response.put("percentageScore", result.getPercentageScore());
        response.put("passed", result.getPassed());
        response.put("correctAnswers", result.getCorrectAnswers());
        response.put("totalQuestions", result.getTotalQuestions());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/test-sessions/{sessionId}/submissions
     * Get all submissions for a session (for review/feedback)
     */
    @GetMapping("/{sessionId}/submissions")
    public ResponseEntity<List<Submission>> getSessionSubmissions(@PathVariable Long sessionId) {
        List<Submission> submissions = evaluationService.getSessionSubmissions(sessionId);
        return ResponseEntity.ok(submissions);
    }

    /**
     * GET /api/v1/test-sessions/{sessionId}/result
     * Get final result for completed test
     */
    @GetMapping("/{sessionId}/result")
    public ResponseEntity<Result> getTestResult(@PathVariable Long sessionId) {
        Result result = evaluationService.getTestResult(sessionId);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/test-sessions/assessment/{assessmentId}
     * Recruiter gets all sessions for assessment (for leaderboard/review)
     */
    @GetMapping("/assessment/{assessmentId}")
    public ResponseEntity<List<TestSession>> getAssessmentSessions(@PathVariable Long assessmentId) {
        List<TestSession> sessions = testSessionService.getAssessmentSessions(assessmentId);
        return ResponseEntity.ok(sessions);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private Map<String, Object> toCandidateQuestion(Question question) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", question.getId());
        item.put("title", question.getTitle());
        item.put("description", question.getDescription());
        item.put("type", question.getType());
        item.put("marks", question.getMarks());
        item.put("sequenceNumber", question.getSequenceNumber());
        item.put("difficulty", question.getDifficulty());
        item.put("option1", question.getOption1());
        item.put("option2", question.getOption2());
        item.put("option3", question.getOption3());
        item.put("option4", question.getOption4());
        item.put("codeTemplate", question.getCodeTemplate());
        item.put("programmingLanguage", question.getProgrammingLanguage());
        item.put("testCases", question.getSampleTestCases() != null ? question.getSampleTestCases() : question.getTestCases());
        item.put("sampleTestCases", question.getSampleTestCases() != null ? question.getSampleTestCases() : question.getTestCases());
        item.put("functionSignature", question.getFunctionSignature());
        item.put("constraintsText", question.getConstraintsText());
        return item;
    }
}
