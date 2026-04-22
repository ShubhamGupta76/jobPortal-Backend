package com.job_Portal_Backend.job_portal_backend.controller;

import com.job_Portal_Backend.job_portal_backend.dto.InterviewCreateDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewUpdateDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewDto> createInterview(
            @Valid @RequestBody InterviewCreateDto createDto,
            @AuthenticationPrincipal User recruiter) {

        InterviewDto interview = interviewService.createInterview(createDto, recruiter);
        return ResponseEntity.ok(interview);
    }

    @PutMapping("/{interviewId}")
    public ResponseEntity<InterviewDto> updateInterview(
            @PathVariable Long interviewId,
            @Valid @RequestBody InterviewUpdateDto updateDto,
            @AuthenticationPrincipal User currentUser) {

        InterviewDto interview = interviewService.updateInterview(interviewId, updateDto, currentUser);
        return ResponseEntity.ok(interview);
    }

    @GetMapping("/{interviewId}")
    public ResponseEntity<InterviewDto> getInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User currentUser) {

        InterviewDto interview = interviewService.getInterviewById(interviewId, currentUser);
        return ResponseEntity.ok(interview);
    }

    @DeleteMapping("/{interviewId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User currentUser) {

        interviewService.deleteInterview(interviewId, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/candidate")
    public ResponseEntity<Page<InterviewDto>> getCandidateInterviews(
            @AuthenticationPrincipal User candidate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<InterviewDto> interviews = interviewService.getInterviewsByCandidate(candidate, pageable);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<Page<InterviewDto>> getRecruiterInterviews(
            @AuthenticationPrincipal User recruiter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<InterviewDto> interviews = interviewService.getInterviewsByRecruiter(recruiter, pageable);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/upcoming/candidate")
    public ResponseEntity<List<InterviewDto>> getUpcomingCandidateInterviews(
            @AuthenticationPrincipal User candidate) {

        List<InterviewDto> interviews = interviewService.getUpcomingInterviewsByCandidate(candidate);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/upcoming/recruiter")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<InterviewDto>> getUpcomingRecruiterInterviews(
            @AuthenticationPrincipal User recruiter) {

        List<InterviewDto> interviews = interviewService.getUpcomingInterviewsByRecruiter(recruiter);
        return ResponseEntity.ok(interviews);
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<List<InterviewDto>> getInterviewsByJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal User currentUser) {

        List<InterviewDto> interviews = interviewService.getInterviewsByJob(jobId, currentUser);
        return ResponseEntity.ok(interviews);
    }

    @PostMapping("/{interviewId}/confirm")
    public ResponseEntity<InterviewDto> confirmInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User candidate) {

        InterviewDto interview = interviewService.confirmInterview(interviewId, candidate);
        return ResponseEntity.ok(interview);
    }

    @PostMapping("/{interviewId}/cancel")
    public ResponseEntity<InterviewDto> cancelInterview(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User currentUser) {

        InterviewDto interview = interviewService.cancelInterview(interviewId, currentUser);
        return ResponseEntity.ok(interview);
    }

    @PostMapping("/{interviewId}/complete")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewDto> markAsCompleted(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User recruiter) {

        InterviewDto interview = interviewService.markAsCompleted(interviewId, recruiter);
        return ResponseEntity.ok(interview);
    }

    @PostMapping("/{interviewId}/no-show")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewDto> markAsNoShow(
            @PathVariable Long interviewId,
            @AuthenticationPrincipal User recruiter) {

        InterviewDto interview = interviewService.markAsNoShow(interviewId, recruiter);
        return ResponseEntity.ok(interview);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<InterviewDto>> getOverdueInterviews() {
        List<InterviewDto> interviews = interviewService.getOverdueInterviews();
        return ResponseEntity.ok(interviews);
    }
}