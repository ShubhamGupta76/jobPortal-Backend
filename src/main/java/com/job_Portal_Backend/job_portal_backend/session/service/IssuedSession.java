package com.job_Portal_Backend.job_portal_backend.session.service;

/** Internal carrier for a freshly issued session: the raw refresh token is returned exactly once. */
public record IssuedSession(Long sessionId, String rawRefreshToken) {
}
