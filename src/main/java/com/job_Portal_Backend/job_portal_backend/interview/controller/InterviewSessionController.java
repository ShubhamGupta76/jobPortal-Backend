package com.job_Portal_Backend.job_portal_backend.interview.controller;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.interview.dto.*;
import com.job_Portal_Backend.job_portal_backend.interview.service.InterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interview-sessions")
@RequiredArgsConstructor
public class InterviewSessionController {

    private final InterviewSessionService interviewSessionService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewSessionResponse> createSession(
            @Valid @RequestBody InterviewSessionCreateRequest request,
            @AuthenticationPrincipal User recruiter) {
        return ResponseEntity.ok(interviewSessionService.createInterviewSession(request, recruiter));
    }

    @GetMapping
    public ResponseEntity<List<InterviewSessionResponse>> mySessions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.getMySessions(currentUser));
    }

    @GetMapping("/{roomToken}")
    public ResponseEntity<InterviewSessionResponse> getSession(
            @PathVariable String roomToken,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.getSession(roomToken, currentUser));
    }

    @GetMapping("/invite/{inviteToken}")
    public ResponseEntity<InterviewSessionResponse> getByInvite(
            @PathVariable String inviteToken,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.getSessionByInviteToken(inviteToken, currentUser));
    }

    @PostMapping("/{roomToken}/join")
    public ResponseEntity<InterviewSessionResponse> join(
            @PathVariable String roomToken,
            @RequestBody JoinInterviewRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.joinInterview(roomToken, request, currentUser));
    }

    @PostMapping("/{roomToken}/leave")
    public ResponseEntity<InterviewSessionResponse> leave(
            @PathVariable String roomToken,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.leaveInterview(roomToken, currentUser));
    }

    @PostMapping("/{roomToken}/start")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewSessionResponse> start(
            @PathVariable String roomToken,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.startInterview(roomToken, currentUser));
    }

    @PostMapping("/{roomToken}/end")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewSessionResponse> end(
            @PathVariable String roomToken,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.endInterview(roomToken, currentUser));
    }

    @PostMapping("/{roomToken}/messages")
    public ResponseEntity<InterviewMessageDto> sendMessage(
            @PathVariable String roomToken,
            @Valid @RequestBody InterviewMessageRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.sendChatMessage(roomToken, request, currentUser));
    }

    @PatchMapping("/{roomToken}/participant-state")
    public ResponseEntity<InterviewSessionResponse> updateState(
            @PathVariable String roomToken,
            @RequestBody ParticipantStateRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.updateParticipantState(roomToken, request, currentUser));
    }

    @PatchMapping("/{roomToken}/recording")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewSessionResponse> recording(
            @PathVariable String roomToken,
            @RequestBody RecordingRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.recordInterview(roomToken, request, currentUser));
    }

    @DeleteMapping("/{roomToken}/participants/{userId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<InterviewSessionResponse> removeParticipant(
            @PathVariable String roomToken,
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(interviewSessionService.removeParticipant(roomToken, userId, currentUser));
    }
}
