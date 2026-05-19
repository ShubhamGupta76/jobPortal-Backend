package com.job_Portal_Backend.job_portal_backend.interview.dto;

import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewRoundType;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewSessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record InterviewSessionResponse(
        Long id,
        String roomToken,
        String inviteToken,
        String inviteUrl,
        String title,
        Long jobId,
        String jobTitle,
        Long recruiterId,
        String recruiterName,
        Long candidateId,
        String candidateName,
        String candidateEmail,
        InterviewRoundType roundType,
        InterviewSessionStatus status,
        LocalDateTime scheduledStartAt,
        LocalDateTime scheduledEndAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer durationMinutes,
        Boolean candidateCanJoinOnce,
        Boolean recordingEnabled,
        Boolean recordingRequested,
        Boolean identityVerificationRequired,
        List<InterviewParticipantDto> participants
) {
}
