package com.job_Portal_Backend.job_portal_backend.interview.mapper;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.interview.dto.InterviewMessageDto;
import com.job_Portal_Backend.job_portal_backend.interview.dto.InterviewParticipantDto;
import com.job_Portal_Backend.job_portal_backend.interview.dto.InterviewSessionResponse;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewMessage;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewParticipant;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewSession;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InterviewSessionMapper {

    public InterviewSessionResponse toResponse(InterviewSession session, List<InterviewParticipant> participants) {
        return new InterviewSessionResponse(
                session.getId(),
                session.getRoomToken(),
                session.getInviteToken(),
                "/interview/join/" + session.getInviteToken(),
                session.getTitle(),
                session.getJob().getId(),
                session.getJob().getTitle(),
                session.getRecruiter().getId(),
                displayName(session.getRecruiter()),
                session.getCandidate().getId(),
                displayName(session.getCandidate()),
                session.getCandidate().getEmail(),
                session.getRoundType(),
                session.getStatus(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getDurationMinutes(),
                session.getCandidateCanJoinOnce(),
                session.getRecordingEnabled(),
                session.getRecordingRequested(),
                session.getIdentityVerificationRequired(),
                participants.stream().map(this::toParticipantDto).toList());
    }

    public InterviewParticipantDto toParticipantDto(InterviewParticipant participant) {
        return new InterviewParticipantDto(
                participant.getId(),
                participant.getUser().getId(),
                displayName(participant.getUser()),
                participant.getUser().getEmail(),
                participant.getRole(),
                participant.getStatus(),
                participant.getMicrophoneMuted(),
                participant.getCameraOff(),
                participant.getScreenSharing(),
                participant.getNetworkQuality(),
                participant.getJoinCount(),
                participant.getJoinedAt(),
                participant.getLastSeenAt());
    }

    public InterviewMessageDto toMessageDto(InterviewMessage message) {
        return new InterviewMessageDto(
                message.getId(),
                message.getSender().getId(),
                displayName(message.getSender()),
                message.getType(),
                message.getContent(),
                message.getCreatedAt());
    }

    private String displayName(User user) {
        String fullName = String.join(" ",
                user.getFirstName() == null ? "" : user.getFirstName(),
                user.getLastName() == null ? "" : user.getLastName()).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
