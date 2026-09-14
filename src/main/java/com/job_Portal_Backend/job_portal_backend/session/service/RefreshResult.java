package com.job_Portal_Backend.job_portal_backend.session.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;

/** Internal carrier for the outcome of a successful refresh-token rotation. */
public record RefreshResult(User user, String accessToken, String rawRefreshToken, Long sessionId) {
}
