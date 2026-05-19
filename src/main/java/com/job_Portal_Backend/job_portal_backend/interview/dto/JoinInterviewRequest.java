package com.job_Portal_Backend.job_portal_backend.interview.dto;

public record JoinInterviewRequest(
        String inviteToken,
        String deviceMetadata,
        String browserFingerprint,
        Boolean identityConfirmed
) {
}
