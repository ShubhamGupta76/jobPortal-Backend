package com.job_Portal_Backend.job_portal_backend.assessments.controller;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.*;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/results")
public class ResultController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private ProctoringService proctoringService;

    /**
     * GET /api/v1/results/{sessionId}
     * Candidate/Recruiter gets result for a test session
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getTestResult(@PathVariable Long sessionId) {
        Result result = evaluationService.getTestResult(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("totalScore", result.getTotalMarksObtained());
        response.put("maxScore", result.getMaxMarks());
        response.put("percentageScore", result.getPercentageScore());
        response.put("passed", result.getPassed());
        response.put("correctAnswers", result.getCorrectAnswers());
        response.put("totalQuestions", result.getTotalQuestions());
        response.put("analysis", result.getDetailedAnalysis());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/results/assessment/{assessmentId}/leaderboard
     * Get leaderboard for assessment (all candidates ranked by score)
     * Recruiter can view this to compare candidate performance
     */
    @GetMapping("/assessment/{assessmentId}/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @PathVariable Long assessmentId) {

        List<Result> results = evaluationService.getAssessmentLeaderboard(assessmentId);

        List<LeaderboardEntryDto> leaderboard = new ArrayList<>();
        Integer rank = 1;

        for (Result result : results) {
            LeaderboardEntryDto entry = new LeaderboardEntryDto();
            entry.setRank(rank);
            entry.setScore(result.getTotalMarksObtained());
            entry.setPercentageScore(result.getPercentageScore());
            entry.setPassed(result.getPassed());
            entry.setCompletedAt(result.getUpdatedAt().toString());
            // TODO: Get candidate name from User entity
            entry.setCandidateName("Candidate " + rank);
            leaderboard.add(entry);
            rank++;
        }

        return ResponseEntity.ok(leaderboard);
    }

    /**
     * GET /api/v1/results?assessmentId={assessmentId}&page={page}
     * Recruiter dashboard: paginated results for assessment
     */
    @GetMapping
    public ResponseEntity<List<Result>> getAssessmentResults(
            @RequestParam Long assessmentId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {

        List<Result> allResults = evaluationService.getAssessmentLeaderboard(assessmentId);

        // Simple pagination
        Integer startIdx = page * limit;
        Integer endIdx = Math.min(startIdx + limit, allResults.size());

        if (startIdx >= allResults.size()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<Result> paginatedResults = allResults.subList(startIdx, endIdx);
        return ResponseEntity.ok(paginatedResults);
    }

    /**
     * GET /api/v1/results/{sessionId}/proctor-report
     * Recruiter gets proctoring report for flagged tests
     */
    @GetMapping("/{sessionId}/proctor-report")
    public ResponseEntity<Map<String, Object>> getProctoringReport(@PathVariable Long sessionId) {
        String report = proctoringService.generateProctoringReport(sessionId);
        Integer riskScore = proctoringService.calculateRiskScore(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("riskScore", riskScore);
        response.put("shouldFlag", proctoringService.shouldFlagResult(sessionId, 3));
        response.put("report", report);

        return ResponseEntity.ok(response);
    }
}
