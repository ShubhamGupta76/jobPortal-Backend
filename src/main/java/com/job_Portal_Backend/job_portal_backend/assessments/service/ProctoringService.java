package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ProctoringService {

    @Autowired
    private ProctoringLogRepository proctoringLogRepository;

    @Autowired
    private TestSessionRepository testSessionRepository;

    /**
     * Log suspicious activity during test.
     * Called by frontend when tab switch, window blur, copy-paste, etc. detected.
     */
    public ProctoringLog logSuspiciousActivity(String sessionToken, String activityType,
            Integer severityScore, String metadata) {
        TestSession testSession = testSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Invalid session token"));

        ProctoringLog log = new ProctoringLog();
        log.setTestSession(testSession);
        log.setActivityType(ProctoringLog.SuspiciousActivity.valueOf(activityType));
        log.setSeverityScore(severityScore);
        log.setMetadata(metadata);
        log.setCreatedAt(LocalDateTime.now());

        return proctoringLogRepository.save(log);
    }

    /**
     * Get all suspicious activities logged for a test session
     */
    public List<ProctoringLog> getSessionLogs(Long testSessionId) {
        return proctoringLogRepository.findByTestSessionId(testSessionId);
    }

    /**
     * Calculate proctoring risk score for test session
     * (0-100, higher = more suspicious)
     */
    public Integer calculateRiskScore(Long testSessionId) {
        Long highSeverityCount = proctoringLogRepository
                .countByTestSessionIdAndSeverityScoreGreaterThan(testSessionId, 50);

        // Simple formula: each high-severity activity adds points
        Integer riskScore = Math.min(100, (int) (highSeverityCount * 15));
        return riskScore;
    }

    /**
     * Flag test session if proctoring violations exceed threshold
     * Called by recruiter review dashboard
     */
    public Boolean shouldFlagResult(Long testSessionId, Integer violationThreshold) {
        Long violationCount = proctoringLogRepository
                .countByTestSessionIdAndSeverityScoreGreaterThan(testSessionId, 70);
        return violationCount >= violationThreshold;
    }

    /**
     * Generate proctoring report for recruiter (summary of suspicious activities)
     */
    public String generateProctoringReport(Long testSessionId) {
        List<ProctoringLog> logs = getSessionLogs(testSessionId);

        StringBuilder report = new StringBuilder();
        report.append("PROCTORING REPORT\n");
        report.append("================\n");
        report.append("Total Events: ").append(logs.size()).append("\n");
        report.append("Risk Score: ").append(calculateRiskScore(testSessionId)).append("/100\n\n");
        report.append("Suspicious Activities:\n");

        Map<ProctoringLog.SuspiciousActivity, Integer> activityCount = new HashMap<>();
        for (ProctoringLog log : logs) {
            activityCount.merge(log.getActivityType(), 1, Integer::sum);
        }

        for (Map.Entry<ProctoringLog.SuspiciousActivity, Integer> entry : activityCount.entrySet()) {
            report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" times\n");
        }

        return report.toString();
    }

    /**
     * Available suspicious activity types enum
     * Sent by frontend when any of these events occur
     */
    public enum ActivityType {
        TAB_SWITCH,
        WINDOW_BLUR,
        COPY_PASTE_DETECTED,
        MULTIPLE_WINDOWS,
        NETWORK_CHANGE,
        DEVICE_DISCONNECT,
        SCREEN_SHARE,
        KEYBOARD_INACTIVE,
        CAMERA_DETECTED_MISSING,
        BACKGROUND_NOISE,
        FACE_NOT_DETECTED
    }
}
