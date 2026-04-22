package com.job_Portal_Backend.job_portal_backend.assessments.controller;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.*;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/proctoring")
public class ProctoringController {

    @Autowired
    private ProctoringService proctoringService;

    /**
     * POST /api/v1/proctoring/log-event
     * Candidate's browser logs suspicious activity during test
     * Called from frontend when tab switch, copy-paste, etc. detected
     */
    @PostMapping("/log-event")
    public ResponseEntity<Map<String, Object>> logSuspiciousEvent(
            @RequestBody ProctoringEventRequest request) {

        ProctoringLog log = proctoringService.logSuspiciousActivity(
                request.getSessionToken(),
                request.getEventType(),
                request.getSeverityScore(),
                request.getMetadata());

        Map<String, Object> response = new HashMap<>();
        response.put("logged", true);
        response.put("severity", log.getSeverityScore());
        response.put("timestamp", log.getCreatedAt());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/proctoring/{sessionId}/logs
     * Recruiter views all proctoring logs for a test session
     */
    @GetMapping("/{sessionId}/logs")
    public ResponseEntity<List<ProctoringLog>> getSessionLogs(@PathVariable Long sessionId) {
        List<ProctoringLog> logs = proctoringService.getSessionLogs(sessionId);
        return ResponseEntity.ok(logs);
    }

    /**
     * GET /api/v1/proctoring/{sessionId}/risk-score
     * Get calculated risk score for a test session (0-100)
     */
    @GetMapping("/{sessionId}/risk-score")
    public ResponseEntity<Map<String, Object>> getRiskScore(@PathVariable Long sessionId) {
        Integer riskScore = proctoringService.calculateRiskScore(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("riskScore", riskScore);
        response.put("flagged", riskScore > 70);
        response.put("riskLevel", riskScore > 70 ? "HIGH" : (riskScore > 40 ? "MEDIUM" : "LOW"));

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/proctoring/{sessionId}/summary
     * Summary of proctoring data for test session
     */
    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<Map<String, Object>> getProcteringSummary(@PathVariable Long sessionId) {
        List<ProctoringLog> logs = proctoringService.getSessionLogs(sessionId);
        Integer riskScore = proctoringService.calculateRiskScore(sessionId);

        // Count activity types
        Map<String, Integer> activityCounts = new HashMap<>();
        for (ProctoringLog log : logs) {
            String activity = log.getActivityType().toString();
            activityCounts.put(activity, activityCounts.getOrDefault(activity, 0) + 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("totalEvents", logs.size());
        response.put("riskScore", riskScore);
        response.put("activities", activityCounts);
        response.put("shouldReviewByRecruiter", riskScore > 60 || logs.size() > 10);

        return ResponseEntity.ok(response);
    }
}
