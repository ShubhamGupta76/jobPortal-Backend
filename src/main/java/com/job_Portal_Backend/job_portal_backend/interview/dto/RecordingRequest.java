package com.job_Portal_Backend.job_portal_backend.interview.dto;

public record RecordingRequest(
        Boolean requested,
        String storageKey
) {
}
