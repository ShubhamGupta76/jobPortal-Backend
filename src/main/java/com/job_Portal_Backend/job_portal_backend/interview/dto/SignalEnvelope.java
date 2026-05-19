package com.job_Portal_Backend.job_portal_backend.interview.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record SignalEnvelope(
        String type,
        String roomToken,
        Long senderId,
        Long targetUserId,
        Map<String, Object> payload,
        LocalDateTime sentAt
) {
}
