package com.job_Portal_Backend.job_portal_backend.session.service;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.entity.UserSession;
import com.job_Portal_Backend.job_portal_backend.repository.UserSessionRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.session.dto.SessionDto;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserSessionServiceTest {

    private final UserSessionRepository sessionRepository = mock(UserSessionRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final UserSessionService service = new UserSessionService(sessionRepository, jwtService, auditLogService);

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenExpiryDays", 30L);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Chrome/120.0 Windows NT 10.0");
        when(request.getRemoteAddr()).thenReturn("203.0.113.5");
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User user(Long id) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@example.com");
        return u;
    }

    @Test
    void issueSessionNeverStoresTheRawToken() {
        User user = user(1L);
        when(sessionRepository.save(any(UserSession.class))).thenAnswer(invocation -> {
            UserSession s = invocation.getArgument(0);
            s.setId(99L);
            return s;
        });

        IssuedSession issued = service.issueSession(user, request);

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(captor.capture());
        UserSession saved = captor.getValue();

        assertNotNull(issued.rawRefreshToken());
        assertNotEquals(issued.rawRefreshToken(), saved.getRefreshTokenHash());
        assertEquals(64, saved.getRefreshTokenHash().length()); // hex-encoded SHA-256
        assertEquals("Chrome on Windows", saved.getDeviceLabel());
        assertEquals("203.0.113.5", saved.getIpAddress());
    }

    @Test
    void refreshRotatesTokenAndIssuesNewAccessToken() {
        User user = user(1L);
        UserSession session = new UserSession();
        session.setId(5L);
        session.setUser(user);
        session.setRefreshTokenHash(sha256("old-raw-token"));
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(sessionRepository.findByRefreshTokenHash(sha256("old-raw-token"))).thenReturn(Optional.of(session));
        when(jwtService.generateToken(user)).thenReturn("new-jwt");

        RefreshResult result = service.refresh("old-raw-token", request);

        assertEquals("new-jwt", result.accessToken());
        assertEquals(5L, result.sessionId());
        assertNotEquals("old-raw-token", result.rawRefreshToken());
        assertEquals(sha256("old-raw-token"), session.getPreviousRefreshTokenHash());
        assertNotEquals(sha256("old-raw-token"), session.getRefreshTokenHash());
    }

    @Test
    void refreshRejectsExpiredSession() {
        User user = user(1L);
        UserSession session = new UserSession();
        session.setId(5L);
        session.setUser(user);
        session.setRefreshTokenHash(sha256("old-raw-token"));
        session.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(sessionRepository.findByRefreshTokenHash(sha256("old-raw-token"))).thenReturn(Optional.of(session));

        assertThrows(ResponseStatusException.class, () -> service.refresh("old-raw-token", request));
    }

    @Test
    void refreshRejectsRevokedSession() {
        User user = user(1L);
        UserSession session = new UserSession();
        session.setId(5L);
        session.setUser(user);
        session.setRefreshTokenHash(sha256("old-raw-token"));
        session.setExpiresAt(LocalDateTime.now().plusDays(1));
        session.setRevokedAt(LocalDateTime.now());

        when(sessionRepository.findByRefreshTokenHash(sha256("old-raw-token"))).thenReturn(Optional.of(session));

        assertThrows(ResponseStatusException.class, () -> service.refresh("old-raw-token", request));
    }

    @Test
    void replayingAnAlreadyRotatedRefreshTokenRevokesTheSessionAndAudits() {
        User user = user(1L);
        UserSession session = new UserSession();
        session.setId(5L);
        session.setUser(user);
        session.setRefreshTokenHash(sha256("current-token"));
        session.setPreviousRefreshTokenHash(sha256("stolen-old-token"));
        session.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(sessionRepository.findByRefreshTokenHash(sha256("stolen-old-token"))).thenReturn(Optional.empty());
        when(sessionRepository.findByPreviousRefreshTokenHash(sha256("stolen-old-token")))
                .thenReturn(Optional.of(session));

        assertThrows(ResponseStatusException.class, () -> service.refresh("stolen-old-token", request));

        assertNotNull(session.getRevokedAt());
        assertEquals("REUSE_DETECTED", session.getRevokedReason());
        verify(auditLogService).logAction(eq("UserSession"), eq(5L), eq("REFRESH_TOKEN_REUSE_DETECTED"),
                eq(user), anyString(), anyString(), anyString());
    }

    @Test
    void unknownRefreshTokenIsRejectedWithoutRevealingWhy() {
        when(sessionRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());
        when(sessionRepository.findByPreviousRefreshTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.refresh("never-issued", request));
    }

    @Test
    void revokeSessionIsScopedToOwner() {
        User user = user(1L);
        when(sessionRepository.findByIdAndUser(5L, user)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.revokeSession(user, 5L));
    }

    @Test
    void revokeOthersExcludesTheCurrentSession() {
        User user = user(1L);
        when(sessionRepository.findNonRevokedByUserExcept(user, 5L)).thenReturn(List.of(new UserSession()));

        service.revokeOthers(user, 5L);

        verify(sessionRepository).findNonRevokedByUserExcept(user, 5L);
        verify(sessionRepository, never()).findNonRevokedByUser(user);
    }

    @Test
    void listSessionsMarksTheCallerSuppliedCurrentSession() {
        User user = user(1L);
        UserSession s1 = new UserSession();
        s1.setId(1L);
        s1.setUser(user);
        UserSession s2 = new UserSession();
        s2.setId(2L);
        s2.setUser(user);
        when(sessionRepository.findActiveByUser(eq(user), any())).thenReturn(List.of(s1, s2));

        List<SessionDto> dtos = service.listSessions(user, 2L);

        assertFalse(dtos.get(0).isCurrent());
        assertTrue(dtos.get(1).isCurrent());
    }

    private static String sha256(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
