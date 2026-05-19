package com.job_Portal_Backend.job_portal_backend.interview.dto;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewParticipantRole;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewParticipantStatus;

import java.time.LocalDateTime;

public record InterviewParticipantDto(
        Long id,
        Long userId,
        String name,
        String email,
        InterviewParticipantRole role,
        InterviewParticipantStatus status,
        Boolean microphoneMuted,
        Boolean cameraOff,
        Boolean screenSharing,
        String networkQuality,
        Integer joinCount,
        LocalDateTime joinedAt,
        LocalDateTime lastSeenAt
) {
}
