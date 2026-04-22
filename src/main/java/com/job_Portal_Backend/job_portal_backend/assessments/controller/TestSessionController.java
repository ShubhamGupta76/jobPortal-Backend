package com.job_Portal_Backend.job_portal_backend.assessments.controller;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.service.*;
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

    /**
     * POST /api/v1/test-sessions/start
     * Candidate starts a test. Returns session token for subsequent requests.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startTestSession(
            @RequestBody Map<String, Long> req,
            Authentication auth,
            HttpServletRequest request) {

        Long assessmentId = req.get("assessmentId");
        User user = (User) auth.getPrincipal();
        System.out.println("Assessment ID: " + assessmentId);
        System.out.println("User ID: " + user.getId());

        String userAgent = request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "";
        String ipAddress = getClientIp(request);

        TestSession session = testSessionService.startTestSession(
                assessmentId, user.getId(), userAgent, ipAddress);

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
        response.put("durationMinutes", session.getAssessment().getDurationMinutes());
        response.put("totalMarks", session.getAssessment().getTotalMarks());
        response.put("status", session.getStatus().toString());
        response.put("expiresAt", session.getExpiresAt());

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
}
