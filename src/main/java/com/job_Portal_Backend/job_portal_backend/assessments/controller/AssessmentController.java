package com.job_Portal_Backend.job_portal_backend.assessments.controller;

import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentSummaryDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentCreateRequest;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssessmentDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.QuestionDto;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.AssignAssessmentRequest;
import com.job_Portal_Backend.job_portal_backend.assessments.dto.UserDto;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.*;
import com.job_Portal_Backend.job_portal_backend.assessments.service.*;
import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.applications.service.ApplicationService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    /**
     * POST /api/v1/assessments
     * Recruiter creates new assessment (test)
     */
@PostMapping
   public ResponseEntity<AssessmentSummaryDto> createAssessment(@RequestBody AssessmentCreateRequest dto,
             @RequestHeader("Authorization") String token) {
                       User recruiter = resolveUser(token);
                               Assessment created = assessmentService.createAssessment(dto, recruiter);
                                      return ResponseEntity.ok(AssessmentSummaryDto.fromAssessment(created));
                                       }

    /**
     * GET /api/v1/assessments
     * GET /api/v1/assessments/my
     * Recruiter views all their created assessments
     */
    @GetMapping({ "", "/my" })
    public ResponseEntity<List<AssessmentSummaryDto>> getRecruiterAssessments(
            @RequestHeader("Authorization") String token) {
        Long recruiterId = extractUserIdFromToken(token);
        List<AssessmentSummaryDto> assessments = assessmentService.getRecruiterAssessments(recruiterId);
        return ResponseEntity.ok(assessments);
    }

    /**
     * GET /api/v1/assessments/{id}
     * Get assessment details with all questions
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssessmentDto> getAssessmentDetails(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentDetails(id));
    }

    /**
     * PUT /api/v1/assessments/{id}
     * Recruiter updates assessment (only if DRAFT)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<AssessmentDto> updateAssessment(@PathVariable Long id,
            @RequestBody AssessmentCreateRequest assessment,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Assessment existing = assessmentService.getAssessmentEntity(id);
        if (!recruiter.getId().equals(existing.getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to update this assessment");
        }
        assessmentService.updateAssessment(id, assessment);
        return ResponseEntity.ok(assessmentService.getAssessmentDetails(id));
    }

    /**
     * POST /api/v1/assessments/{id}/publish
     * Recruiter publishes assessment to make visible to candidates
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<AssessmentDto> publishAssessment(@PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Assessment existing = assessmentService.getAssessmentEntity(id);
        if (!recruiter.getId().equals(existing.getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to publish this assessment");
        }
        assessmentService.publishAssessment(id);
        return ResponseEntity.ok(assessmentService.getAssessmentDetails(id));
    }

    /**
     * POST /api/v1/assessments/{id}/close
     * Recruiter closes assessment (no more new attempts)
     */
    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<AssessmentDto> closeAssessment(@PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Assessment existing = assessmentService.getAssessmentEntity(id);
        if (!recruiter.getId().equals(existing.getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to close this assessment");
        }
        assessmentService.closeAssessment(id);
        return ResponseEntity.ok(assessmentService.getAssessmentDetails(id));
    }

    /**
     * DELETE /api/v1/assessments/{id}
     * Delete assessment (only DRAFT)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Assessment existing = assessmentService.getAssessmentEntity(id);
        if (!recruiter.getId().equals(existing.getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to delete this assessment");
        }
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/assessments/job/{jobId}
     * Get available assessments for a job that candidates can take
     */
    /**
     * GET /api/v1/assessments/job/{jobId}
     * Get available assessments for a job that candidates can take
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<AssessmentSummaryDto>> getAssessmentsForJob(@PathVariable Long jobId) {
        List<AssessmentSummaryDto> assessments = assessmentService.getAvailableAssessmentsForJob(jobId);
        return ResponseEntity.ok(assessments);
    }

    @PostMapping("/{assessmentId}/questions")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Question> addQuestion(
            @PathVariable Long assessmentId,
            @RequestBody Question question,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Assessment assessment = assessmentService.getAssessmentEntity(assessmentId);
        if (!recruiter.getId().equals(assessment.getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to add questions to this assessment");
        }
        question.setAssessment(assessment);
        Question created = assessmentService.addQuestion(question);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody Question question,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Question existing = assessmentService.getQuestionEntity(questionId);
        if (!recruiter.getId().equals(existing.getAssessment().getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to update this question");
        }
        question.setAssessment(existing.getAssessment());
        question.setId(existing.getId());
        return ResponseEntity.ok(assessmentService.updateQuestion(questionId, question));
    }

    @DeleteMapping("/questions/{questionId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long questionId,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        Question existing = assessmentService.getQuestionEntity(questionId);
        if (!recruiter.getId().equals(existing.getAssessment().getRecruiter().getId())) {
            throw new RuntimeException("Not authorized to delete this question");
        }
        assessmentService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<ApplicationDto>> assignAssessment(
            @Valid @RequestBody AssignAssessmentRequest request,
            @RequestHeader("Authorization") String token) {
        User recruiter = resolveUser(token);
        ApplicationDto updated = applicationService.assignAssessment(
                request.getJobId(),
                request.getCandidateId(),
                request.getAssessmentId(),
                recruiter);
        return ResponseEntity.ok(new ApiResponse<>(true, "Assessment assigned successfully", updated));
    }

    private Long extractUserIdFromToken(String token) {
        return jwtService.extractUserId(token.substring(7));
    }

    private User resolveUser(String token) {
        String email = jwtService.extractUsername(token.substring(7));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
