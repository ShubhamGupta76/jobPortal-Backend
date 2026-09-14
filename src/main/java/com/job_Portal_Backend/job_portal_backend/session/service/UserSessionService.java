package com.job_Portal_Backend.job_portal_backend.session.service;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.entity.UserSession;
import com.job_Portal_Backend.job_portal_backend.repository.UserSessionRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.session.dto.SessionDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Additive session/refresh-token layer that runs alongside the existing stateless 24h JWT access
 * token, which is left completely unchanged. Only SHA-256 hashes of refresh tokens are ever
 * persisted (see UserSession); raw tokens exist only in memory for the single response that
 * issues them and are never logged.
 */
@Service
public class UserSessionService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final UserSessionRepository sessionRepository;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.session.refresh-token-expiry-days:30}")
    private long refreshTokenExpiryDays;

    public UserSessionService(UserSessionRepository sessionRepository, JwtService jwtService,
            AuditLogService auditLogService) {
        this.sessionRepository = sessionRepository;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public IssuedSession issueSession(User user, HttpServletRequest request) {
        String rawToken = generateRawToken();
        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hash(rawToken));
        session.setDeviceLabel(UserAgentLabeler.describe(request.getHeader("User-Agent")));
        session.setIpAddress(clientIp(request));
        session.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays));
        UserSession saved = sessionRepository.save(session);
        return new IssuedSession(saved.getId(), rawToken);
    }

    @Transactional
    public RefreshResult refresh(String rawToken, HttpServletRequest request) {
        String incomingHash = hash(rawToken);
        Optional<UserSession> current = sessionRepository.findByRefreshTokenHash(incomingHash);
        if (current.isPresent()) {
            UserSession session = current.get();
            if (!session.isActive()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
            }

            String newRawToken = generateRawToken();
            session.setPreviousRefreshTokenHash(session.getRefreshTokenHash());
            session.setRefreshTokenHash(hash(newRawToken));
            session.setLastUsedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays));
            session.setIpAddress(clientIp(request));
            sessionRepository.save(session);

            String accessToken = jwtService.generateToken(session.getUser());
            return new RefreshResult(session.getUser(), accessToken, newRawToken, session.getId());
        }

        // The incoming token matches a hash that was already rotated away from a real session:
        // this is a replayed, previously-used refresh token. Treat the session as compromised.
        Optional<UserSession> reused = sessionRepository.findByPreviousRefreshTokenHash(incomingHash);
        if (reused.isPresent()) {
            UserSession compromised = reused.get();
            if (compromised.getRevokedAt() == null) {
                compromised.setRevokedAt(LocalDateTime.now());
                compromised.setRevokedReason("REUSE_DETECTED");
                sessionRepository.save(compromised);
                auditLogService.logAction("UserSession", compromised.getId(), "REFRESH_TOKEN_REUSE_DETECTED",
                        compromised.getUser(), "Refresh token reuse detected; session revoked",
                        clientIp(request), request.getHeader("User-Agent"));
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }

    @Transactional
    public void revokeByRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        sessionRepository.findByRefreshTokenHash(hash(rawToken)).ifPresent(session -> {
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedReason("LOGOUT");
            sessionRepository.save(session);
        });
    }

    public List<SessionDto> listSessions(User user, Long currentSessionId) {
        return sessionRepository.findActiveByUser(user, LocalDateTime.now()).stream()
                .map(session -> toDto(session, currentSessionId))
                .toList();
    }

    @Transactional
    public void revokeSession(User user, Long sessionId) {
        UserSession session = sessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokedReason("USER_REVOKED");
        sessionRepository.save(session);
    }

    @Transactional
    public void revokeOthers(User user, Long currentSessionId) {
        List<UserSession> sessions = currentSessionId != null
                ? sessionRepository.findNonRevokedByUserExcept(user, currentSessionId)
                : sessionRepository.findNonRevokedByUser(user);
        LocalDateTime now = LocalDateTime.now();
        sessions.forEach(session -> {
            session.setRevokedAt(now);
            session.setRevokedReason("USER_REVOKED_OTHERS");
        });
        sessionRepository.saveAll(sessions);
    }

    @Transactional
    public void revokeAll(User user) {
        List<UserSession> sessions = sessionRepository.findNonRevokedByUser(user);
        LocalDateTime now = LocalDateTime.now();
        sessions.forEach(session -> {
            session.setRevokedAt(now);
            session.setRevokedReason("USER_REVOKED_ALL");
        });
        sessionRepository.saveAll(sessions);
    }

    private SessionDto toDto(UserSession session, Long currentSessionId) {
        return new SessionDto(
                session.getId(),
                session.getDeviceLabel(),
                session.getIpAddress(),
                session.getCreatedAt(),
                session.getLastUsedAt(),
                session.getExpiresAt(),
                currentSessionId != null && currentSessionId.equals(session.getId()));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
