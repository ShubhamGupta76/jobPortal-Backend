package com.job_Portal_Backend.job_portal_backend.interview.signaling;

import java.util.Map;

public record InterviewSocketMessage(
        String type,
        String roomToken,
        Long targetUserId,
        Map<String, Object> payload
) {
}
