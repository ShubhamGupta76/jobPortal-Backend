package com.job_Portal_Backend.job_portal_backend.interview.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.interview.dto.SignalEnvelope;
import com.job_Portal_Backend.job_portal_backend.interview.entity.InterviewSession;
import com.job_Portal_Backend.job_portal_backend.interview.service.InterviewSessionService;
import com.job_Portal_Backend.job_portal_backend.interview.signaling.InterviewSocketMessage;
import com.job_Portal_Backend.job_portal_backend.interview.signaling.InterviewSocketSessionRegistry;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InterviewSignalingWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final InterviewSessionService interviewSessionService;
    private final InterviewSocketSessionRegistry registry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, String> query = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().toSingleValueMap();
        String token = normalizeToken(query.get("token"));
        String roomToken = query.get("roomToken");

        if (token == null || roomToken == null || roomToken.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token or roomToken"));
            return;
        }

        User user = authenticate(token);
        InterviewSession interviewSession = interviewSessionService.validateRoomAccess(roomToken, user);
        registry.register(roomToken, user.getId(), session);
        session.getAttributes().put("roomToken", roomToken);
        session.getAttributes().put("userId", user.getId());

        interviewSessionService.logActivity(interviewSession, user, "SOCKET_CONNECTED", browserMetadata(session));
        broadcast(roomToken, new SignalEnvelope("participant-online", roomToken, user.getId(), null,
                Map.of("onlineParticipants", registry.participantIds(roomToken)), LocalDateTime.now()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String roomToken = (String) session.getAttributes().get("roomToken");
        if (userId == null || roomToken == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Unauthenticated socket"));
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        interviewSessionService.validateRoomAccess(roomToken, user);

        InterviewSocketMessage socketMessage = objectMapper.readValue(message.getPayload(), InterviewSocketMessage.class);
        SignalEnvelope envelope = new SignalEnvelope(
                socketMessage.type(),
                roomToken,
                userId,
                socketMessage.targetUserId(),
                socketMessage.payload(),
                LocalDateTime.now());

        if (socketMessage.targetUserId() != null) {
            sendTo(roomToken, socketMessage.targetUserId(), envelope);
        } else {
            broadcast(roomToken, envelope);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String roomToken = (String) session.getAttributes().get("roomToken");
        if (userId == null || roomToken == null) {
            return;
        }
        registry.unregister(roomToken, userId);
        broadcast(roomToken, new SignalEnvelope("participant-offline", roomToken, userId, null,
                Map.of("onlineParticipants", registry.participantIds(roomToken)), LocalDateTime.now()));
    }

    private User authenticate(String token) {
        String email = jwtService.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid socket token"));
        if (!jwtService.isTokenValid(token, user) || !user.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid socket token");
        }
        return user;
    }

    private void sendTo(String roomToken, Long userId, SignalEnvelope envelope) throws IOException {
        WebSocketSession target = registry.participants(roomToken).get(userId);
        if (target != null && target.isOpen()) {
            target.sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
        }
    }

    private void broadcast(String roomToken, SignalEnvelope envelope) throws IOException {
        String payload = objectMapper.writeValueAsString(envelope);
        for (WebSocketSession socketSession : registry.participants(roomToken).values()) {
            if (socketSession.isOpen()) {
                socketSession.sendMessage(new TextMessage(payload));
            }
        }
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token.startsWith("Bearer ") ? token.substring(7) : token;
    }

    private String browserMetadata(WebSocketSession session) {
        return session.getHandshakeHeaders().getFirst("user-agent");
    }
}
