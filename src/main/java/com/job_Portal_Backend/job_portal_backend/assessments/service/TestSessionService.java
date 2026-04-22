package com.job_Portal_Backend.job_portal_backend.assessments.service;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class TestSessionService {

    @Autowired
    private TestSessionRepository testSessionRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * Start a new test session for candidate.
     * Generates unique session token, records browser/IP info for proctoring.
     */
    public TestSession startTestSession(Long assessmentId, Long candidateId, String userAgent, String ipAddress) {
        System.out.println("Service - Assessment ID: " + assessmentId + ", Candidate ID: " + candidateId);

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        // Check for existing session (not EXPIRED or EVALUATED)
        List<TestSession> sessions = testSessionRepository.findByAssessmentIdAndCandidateId(assessmentId, candidateId);
        Optional<TestSession> existing = sessions.stream()
                .filter(s -> !s.getStatus().equals(TestSession.SessionStatus.EXPIRED)
                        && !s.getStatus().equals(TestSession.SessionStatus.EVALUATED))
                .findFirst();
        if (existing.isPresent()) {
            System.out.println("Returning existing session: " + existing.get().getId());
            return existing.get();
        }

        // Prevent multiple concurrent IN_PROGRESS sessions (additional safety)
        for (TestSession session : sessions) {
            if (session.getStatus().equals(TestSession.SessionStatus.IN_PROGRESS)) {
                throw new RuntimeException("Candidate already has an active session for this assessment");
            }
        }

        // Create new session with unique token
        TestSession session = new TestSession();
        session.setAssessment(assessment);
        User candidate = new User();
        candidate.setId(candidateId);
        session.setCandidate(candidate);
        session.setSessionToken(generateSecureToken());
        session.setStatus(TestSession.SessionStatus.NOT_STARTED);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setStartedAt(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        // Calculate expiry: now + assessment duration (with 5 min buffer)
        Integer durationMinutes = assessment.getDurationMinutes();
        session.setExpiresAt(LocalDateTime.now().plusMinutes(durationMinutes + 5));

        return testSessionRepository.save(session);
    }

    /**
     * Get current active session for candidate (by token)
     */
    public TestSession getSessionByToken(String sessionToken) {
        return testSessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new RuntimeException("Invalid or expired session token"));
    }

    /**
     * Validate session is active and not expired
     */
    public boolean isSessionValid(String sessionToken) {
        Optional<TestSession> session = testSessionRepository.findBySessionToken(sessionToken);
        if (session.isEmpty()) {
            return false;
        }

        TestSession testSession = session.get();
        LocalDateTime now = LocalDateTime.now();

        // Check status
        if (!testSession.getStatus().equals(TestSession.SessionStatus.IN_PROGRESS) &&
                !testSession.getStatus().equals(TestSession.SessionStatus.NOT_STARTED)) {
            return false;
        }

        // Check expiry
        if (now.isAfter(testSession.getExpiresAt())) {
            testSession.setStatus(TestSession.SessionStatus.EXPIRED);
            testSessionRepository.save(testSession);
            return false;
        }

        return true;
    }

    /**
     * Mark session as IN_PROGRESS when candidate first loads test
     */
    public TestSession markSessionStarted(String sessionToken) {
        TestSession session = getSessionByToken(sessionToken);
        session.setStatus(TestSession.SessionStatus.IN_PROGRESS);
        session.setUpdatedAt(LocalDateTime.now());
        return testSessionRepository.save(session);
    }

    /**
     * Submit test (candidate clicks Final Submit button)
     * Auto-evaluation happens in separate service
     */
    public TestSession submitTest(String sessionToken) {
        TestSession session = getSessionByToken(sessionToken);

        if (!isSessionValid(sessionToken)) {
            throw new RuntimeException("Session is expired or no longer active");
        }

        session.setStatus(TestSession.SessionStatus.SUBMITTED);
        session.setEndedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return testSessionRepository.save(session);
    }

    /**
     * Get submission count for session (for UI progress indicator)
     */
    public Integer getSubmissionCount(Long sessionId) {
        List<Submission> submissions = submissionRepository.findByTestSessionId(sessionId);
        return submissions.size();
    }

    /**
     * Get all sessions for assessment (for recruiter leaderboard)
     */
    public List<TestSession> getAssessmentSessions(Long assessmentId) {
        return testSessionRepository.findByAssessmentId(assessmentId);
    }

    /**
     * Get candidate's session history for assessment
     */
    public List<TestSession> getCandidateSessionHistory(Long assessmentId, Long candidateId) {
        return testSessionRepository.findByAssessmentIdAndCandidateId(assessmentId, candidateId);
    }

    /**
     * Generate secure random token (40 chars alphanumeric)
     */
    private String generateSecureToken() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            token.append(chars.charAt(random.nextInt(chars.length())));
        }
        return token.toString();
    }
}
