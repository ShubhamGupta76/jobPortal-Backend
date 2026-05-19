package com.job_Portal_Backend.job_portal_backend.interview.dto;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewRoundType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record InterviewSessionCreateRequest(
        @NotNull Long jobId,
        @NotNull Long candidateId,
        @NotBlank String title,
        @NotNull @Future LocalDateTime scheduledStartAt,
        @Min(15) Integer durationMinutes,
        InterviewRoundType roundType,
        Boolean candidateCanJoinOnce,
        Boolean recordingEnabled,
        Boolean identityVerificationRequired
) {
}
