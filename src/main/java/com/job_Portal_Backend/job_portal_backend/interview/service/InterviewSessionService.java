package com.job_Portal_Backend.job_portal_backend.interview.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.interview.dto.*;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewSession;

import java.util.List;

public interface InterviewSessionService {
    InterviewSessionResponse createInterviewSession(InterviewSessionCreateRequest request, User recruiter);

    InterviewSessionResponse joinInterview(String roomToken, JoinInterviewRequest request, User currentUser);

    InterviewSessionResponse startInterview(String roomToken, User currentUser);

    InterviewSessionResponse leaveInterview(String roomToken, User currentUser);

    InterviewSessionResponse endInterview(String roomToken, User currentUser);

    InterviewSessionResponse getSession(String roomToken, User currentUser);

    InterviewSessionResponse getSessionByInviteToken(String inviteToken, User currentUser);

    List<InterviewSessionResponse> getMySessions(User currentUser);

    InterviewMessageDto sendChatMessage(String roomToken, InterviewMessageRequest request, User currentUser);

    InterviewSessionResponse updateParticipantState(String roomToken, ParticipantStateRequest request, User currentUser);

    InterviewSessionResponse recordInterview(String roomToken, RecordingRequest request, User currentUser);

    InterviewSessionResponse removeParticipant(String roomToken, Long participantUserId, User currentUser);

    InterviewSession validateRoomAccess(String roomToken, User currentUser);

    void logActivity(InterviewSession session, User actor, String action, String metadata);
}
