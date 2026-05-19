package com.job_Portal_Backend.job_portal_backend.interview.dto;

public record ParticipantStateRequest(
        Boolean microphoneMuted,
        Boolean cameraOff,
        Boolean screenSharing,
        String networkQuality
) {
}
