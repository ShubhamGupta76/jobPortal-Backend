package com.job_Portal_Backend.job_portal_backend.interview.service.impl;

import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.interview.dto.*;
import com.job_Portal_Backend.job_portal_backend.interview.entity.*;
import com.job_Portal_Backend.job_portal_backend.interview.mapper.InterviewSessionMapper;
import com.job_Portal_Backend.job_portal_backend.interview.repository.InterviewActivityLogRepository;
import com.job_Portal_Backend.job_portal_backend.interview.repository.InterviewMessageRepository;
import com.job_Portal_Backend.job_portal_backend.interview.repository.InterviewParticipantRepository;
import com.job_Portal_Backend.job_portal_backend.interview.repository.InterviewSessionRepository;
import com.job_Portal_Backend.job_portal_backend.interview.service.InterviewSessionService;
import com.job_Portal_Backend.job_portal_backend.interview.utils.SecureRoomTokenGenerator;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private static final int EARLY_JOIN_MINUTES = 30;
    private static final int LATE_JOIN_MINUTES = 20;

    private final InterviewSessionRepository sessionRepository;
    private final InterviewParticipantRepository participantRepository;
    private final InterviewMessageRepository messageRepository;
    private final InterviewActivityLogRepository activityLogRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;
    private final SecureRoomTokenGenerator tokenGenerator;
    private final InterviewSessionMapper mapper;

    @Override
    public InterviewSessionResponse createInterviewSession(InterviewSessionCreateRequest request, User recruiter) {
        Job job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!Objects.equals(job.getRecruiter().getId(), recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the job recruiter can create this room");
        }

        User candidate = userRepository.findById(request.candidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        Application application = applicationRepository
                .findByUserIdAndJobIdAndNotDeleted(candidate.getId(), job.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Candidate must apply to this job before an interview can be created"));

        application.setStatus(Application.ApplicationStatus.INTERVIEW);
        int duration = request.durationMinutes() == null ? 60 : request.durationMinutes();

        InterviewSession session = new InterviewSession();
        session.setRoomToken(tokenGenerator.roomToken());
        session.setInviteToken(tokenGenerator.inviteToken());
        session.setTitle(request.title().trim());
        session.setJob(job);
        session.setRecruiter(recruiter);
        session.setCandidate(candidate);
        session.setRoundType(request.roundType() == null ? InterviewRoundType.TECHNICAL : request.roundType());
        session.setScheduledStartAt(request.scheduledStartAt());
        session.setScheduledEndAt(request.scheduledStartAt().plusMinutes(duration));
        session.setDurationMinutes(duration);
        session.setCandidateCanJoinOnce(request.candidateCanJoinOnce() == null || request.candidateCanJoinOnce());
        session.setRecordingEnabled(Boolean.TRUE.equals(request.recordingEnabled()));
        session.setIdentityVerificationRequired(request.identityVerificationRequired() == null
                || request.identityVerificationRequired());

        InterviewSession saved = sessionRepository.save(session);
        participantRepository.save(participant(saved, recruiter, InterviewParticipantRole.RECRUITER));
        participantRepository.save(participant(saved, candidate, InterviewParticipantRole.CANDIDATE));
        logActivity(saved, recruiter, "SESSION_CREATED", "round=" + saved.getRoundType());
        notificationService.sendInterviewScheduled(candidate, recruiter, job.getTitle(), saved.getScheduledStartAt());
        return response(saved);
    }

    @Override
    public InterviewSessionResponse joinInterview(String roomToken, JoinInterviewRequest request, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getScheduledStartAt().minusMinutes(EARLY_JOIN_MINUTES))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview room is not open yet");
        }
        if (now.isAfter(session.getScheduledEndAt().plusMinutes(LATE_JOIN_MINUTES))) {
            session.setStatus(InterviewSessionStatus.EXPIRED);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview session has expired");
        }
        if (session.getStatus() == InterviewSessionStatus.ENDED || session.getStatus() == InterviewSessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview session is closed");
        }
        if (session.getStatus() == InterviewSessionStatus.SCHEDULED && isCandidate(session, currentUser)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recruiter has not started the interview yet");
        }
        if (Boolean.TRUE.equals(session.getIdentityVerificationRequired())
                && isCandidate(session, currentUser)
                && !Boolean.TRUE.equals(request.identityConfirmed())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidate identity confirmation is required");
        }

        InterviewParticipant participant = participantRepository.findBySessionIdAndUserId(session.getId(), currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Participant is not invited"));
        if (participant.getRole() == InterviewParticipantRole.CANDIDATE
                && Boolean.TRUE.equals(session.getCandidateCanJoinOnce())
                && participant.getJoinCount() > 0
                && participant.getStatus() == InterviewParticipantStatus.REMOVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Candidate cannot rejoin this interview");
        }

        participant.setStatus(InterviewParticipantStatus.ONLINE);
        participant.setJoinedAt(participant.getJoinedAt() == null ? now : participant.getJoinedAt());
        participant.setLastSeenAt(now);
        participant.setJoinCount(participant.getJoinCount() + 1);
        participant.setDeviceMetadata(request.deviceMetadata());
        participantRepository.save(participant);

        if (session.getStatus() == InterviewSessionStatus.WAITING) {
            session.setStatus(InterviewSessionStatus.LIVE);
            session.setStartedAt(session.getStartedAt() == null ? now : session.getStartedAt());
        }
        logActivity(session, currentUser, "PARTICIPANT_JOINED", request.deviceMetadata());
        return response(session);
    }

    @Override
    public InterviewSessionResponse startInterview(String roomToken, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        ensureRecruiter(session, currentUser);
        if (session.getStatus() == InterviewSessionStatus.ENDED || session.getStatus() == InterviewSessionStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Interview session is already closed");
        }
        session.setStatus(InterviewSessionStatus.LIVE);
        session.setStartedAt(session.getStartedAt() == null ? LocalDateTime.now() : session.getStartedAt());
        logActivity(session, currentUser, "SESSION_STARTED", "roomToken=" + session.getRoomToken());
        notificationService.sendNotificationToUser(
                session.getCandidate(),
                "Interview Started",
                "Recruiter started your interview for '" + session.getJob().getTitle() + "'. Join from your interviews page.",
                "INTERVIEW_STARTED");
        return response(sessionRepository.save(session));
    }

    @Override
    public InterviewSessionResponse leaveInterview(String roomToken, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        InterviewParticipant participant = participantRepository.findBySessionIdAndUserId(session.getId(), currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Participant is not invited"));
        participant.setStatus(InterviewParticipantStatus.LEFT);
        participant.setLeftAt(LocalDateTime.now());
        participant.setLastSeenAt(LocalDateTime.now());
        participantRepository.save(participant);
        logActivity(session, currentUser, "PARTICIPANT_LEFT", null);
        return response(session);
    }

    @Override
    public InterviewSessionResponse endInterview(String roomToken, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        ensureRecruiter(session, currentUser);
        session.setStatus(InterviewSessionStatus.ENDED);
        session.setEndedAt(LocalDateTime.now());
        logActivity(session, currentUser, "SESSION_ENDED", null);
        return response(sessionRepository.save(session));
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSession(String roomToken, User currentUser) {
        return response(validateRoomAccess(roomToken, currentUser));
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSessionResponse getSessionByInviteToken(String inviteToken, User currentUser) {
        InterviewSession session = sessionRepository.findByInviteTokenAndIsDeletedFalse(inviteToken)
                .orElseThrow(() -> new ResourceNotFoundException("Interview invite not found"));
        ensureParticipant(session, currentUser);
        return response(session);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewSessionResponse> getMySessions(User currentUser) {
        String role = RoleUtils.resolvePrimaryRole(currentUser.getRoles());
        List<InterviewSession> sessions = "RECRUITER".equals(role)
                ? sessionRepository.findByRecruiterIdAndIsDeletedFalseOrderByScheduledStartAtDesc(currentUser.getId())
                : sessionRepository.findByCandidateIdAndIsDeletedFalseOrderByScheduledStartAtDesc(currentUser.getId());
        return sessions.stream().map(this::response).toList();
    }

    @Override
    public InterviewMessageDto sendChatMessage(String roomToken, InterviewMessageRequest request, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        InterviewMessage message = new InterviewMessage();
        message.setSession(session);
        message.setSender(currentUser);
        message.setType(request.type() == null ? InterviewMessageType.CHAT : request.type());
        message.setContent(request.content().trim());
        InterviewMessage saved = messageRepository.save(message);
        logActivity(session, currentUser, "MESSAGE_SENT", "type=" + saved.getType());
        return mapper.toMessageDto(saved);
    }

    @Override
    public InterviewSessionResponse updateParticipantState(String roomToken, ParticipantStateRequest request, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        InterviewParticipant participant = participantRepository.findBySessionIdAndUserId(session.getId(), currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Participant is not invited"));
        if (request.microphoneMuted() != null) participant.setMicrophoneMuted(request.microphoneMuted());
        if (request.cameraOff() != null) participant.setCameraOff(request.cameraOff());
        if (request.screenSharing() != null) participant.setScreenSharing(request.screenSharing());
        if (request.networkQuality() != null) participant.setNetworkQuality(request.networkQuality());
        participant.setLastSeenAt(LocalDateTime.now());
        participantRepository.save(participant);
        return response(session);
    }

    @Override
    public InterviewSessionResponse recordInterview(String roomToken, RecordingRequest request, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        ensureRecruiter(session, currentUser);
        if (!Boolean.TRUE.equals(session.getRecordingEnabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recording is not enabled for this session");
        }
        session.setRecordingRequested(Boolean.TRUE.equals(request.requested()));
        session.setRecordingStorageKey(request.storageKey());
        logActivity(session, currentUser, "RECORDING_UPDATED", "requested=" + session.getRecordingRequested());
        return response(sessionRepository.save(session));
    }

    @Override
    public InterviewSessionResponse removeParticipant(String roomToken, Long participantUserId, User currentUser) {
        InterviewSession session = validateRoomAccess(roomToken, currentUser);
        ensureRecruiter(session, currentUser);
        if (Objects.equals(session.getRecruiter().getId(), participantUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recruiter cannot remove themselves");
        }
        InterviewParticipant participant = participantRepository.findBySessionIdAndUserId(session.getId(), participantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        participant.setStatus(InterviewParticipantStatus.REMOVED);
        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);
        logActivity(session, currentUser, "PARTICIPANT_REMOVED", "userId=" + participantUserId);
        return response(session);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewSession validateRoomAccess(String roomToken, User currentUser) {
        InterviewSession session = sessionRepository.findByRoomTokenAndIsDeletedFalse(roomToken)
                .orElseThrow(() -> new ResourceNotFoundException("Interview room not found"));
        ensureParticipant(session, currentUser);
        return session;
    }

    @Override
    public void logActivity(InterviewSession session, User actor, String action, String metadata) {
        InterviewActivityLog log = new InterviewActivityLog();
        log.setSession(session);
        log.setActor(actor);
        log.setAction(action);
        log.setMetadata(metadata);
        activityLogRepository.save(log);
    }

    private InterviewParticipant participant(InterviewSession session, User user, InterviewParticipantRole role) {
        InterviewParticipant participant = new InterviewParticipant();
        participant.setSession(session);
        participant.setUser(user);
        participant.setRole(role);
        participant.setStatus(InterviewParticipantStatus.INVITED);
        return participant;
    }

    private InterviewSessionResponse response(InterviewSession session) {
        return mapper.toResponse(session, participantRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()));
    }

    private void ensureRecruiter(InterviewSession session, User currentUser) {
        if (!Objects.equals(session.getRecruiter().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only recruiter can perform this action");
        }
    }

    private void ensureParticipant(InterviewSession session, User currentUser) {
        if (!Objects.equals(session.getRecruiter().getId(), currentUser.getId())
                && !Objects.equals(session.getCandidate().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not invited to this interview");
        }
    }

    private boolean isCandidate(InterviewSession session, User user) {
        return Objects.equals(session.getCandidate().getId(), user.getId());
    }
}
