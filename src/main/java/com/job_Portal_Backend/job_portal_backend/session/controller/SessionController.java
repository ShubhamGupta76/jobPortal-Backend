package com.job_Portal_Backend.job_portal_backend.session.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.session.dto.RevokeOthersRequest;
import com.job_Portal_Backend.job_portal_backend.session.dto.SessionDto;
import com.job_Portal_Backend.job_portal_backend.session.service.UserSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * All endpoints here resolve the acting user strictly from the authenticated JWT principal
 * (never from a client-supplied id), and every lookup/mutation is additionally scoped to that
 * user at the repository level (see UserSessionRepository.findByIdAndUser), so one user can never
 * list or revoke another user's sessions.
 */
@RestController
@RequestMapping("/api/v1/auth/sessions")
public class SessionController {

    private final UserSessionService userSessionService;

    public SessionController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @GetMapping
    public ApiResponse<List<SessionDto>> listSessions(@AuthenticationPrincipal User user,
            @RequestParam(required = false) Long currentSessionId) {
        requireUser(user);
        return new ApiResponse<>(true, "Sessions retrieved", userSessionService.listSessions(user, currentSessionId));
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<String> revokeSession(@AuthenticationPrincipal User user, @PathVariable Long sessionId) {
        requireUser(user);
        userSessionService.revokeSession(user, sessionId);
        return new ApiResponse<>(true, "Session revoked", null);
    }

    @PostMapping("/revoke-others")
    public ApiResponse<String> revokeOthers(@AuthenticationPrincipal User user,
            @RequestBody(required = false) RevokeOthersRequest request) {
        requireUser(user);
        Long currentSessionId = request != null ? request.getCurrentSessionId() : null;
        userSessionService.revokeOthers(user, currentSessionId);
        return new ApiResponse<>(true, "Other sessions revoked", null);
    }

    @PostMapping("/revoke-all")
    public ApiResponse<String> revokeAll(@AuthenticationPrincipal User user) {
        requireUser(user);
        userSessionService.revokeAll(user);
        return new ApiResponse<>(true, "All sessions revoked", null);
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
