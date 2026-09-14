package com.job_Portal_Backend.job_portal_backend.auth.service;

import com.job_Portal_Backend.job_portal_backend.auth.dto.AuthResponse;
import com.job_Portal_Backend.job_portal_backend.auth.dto.LoginRequest;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.otp.OtpService;
import com.job_Portal_Backend.job_portal_backend.repository.RoleRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.session.service.IssuedSession;
import com.job_Portal_Backend.job_portal_backend.session.service.UserSessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Login is direct (password-only, no OTP) for a verified, active account — see AuthController's
 * "/login-credentials" handler. OTP remains required only for registration
 * (sendOtpForRegistration / completeRegistrationAfterOtp), which this class does not touch.
 *
 * <p>Also locks in the login-flow account-enumeration / timing side-channel fix: an unknown email
 * and a known email with the wrong password must be indistinguishable to the caller (same
 * exception message), and a BCrypt comparison must run in both cases.
 */
class AuthServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final OtpService otpService = mock(OtpService.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);

    private final AuthService authService = new AuthService(
            userRepository, roleRepository, passwordEncoder, jwtService, otpService, userSessionService);

    private LoginRequest request(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    void unknownEmailAndWrongPasswordProduceTheSameMessage() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        User existing = new User();
        existing.setEmail("real@example.com");
        existing.setPassword("hashed");
        when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(existing));

        RuntimeException unknownUserEx = assertThrows(RuntimeException.class,
                () -> authService.login(request("ghost@example.com", "whatever"), httpRequest));
        RuntimeException wrongPasswordEx = assertThrows(RuntimeException.class,
                () -> authService.login(request("real@example.com", "wrong"), httpRequest));

        assertEquals(unknownUserEx.getMessage(), wrongPasswordEx.getMessage());
        assertEquals("Invalid email or password", unknownUserEx.getMessage());
    }

    @Test
    void unknownEmailStillRunsABCryptComparisonAgainstADummyHash() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request("ghost@example.com", "x"), httpRequest));

        verify(passwordEncoder).matches(eq("x"), anyString());
    }

    @Test
    void blockedStatusIsOnlyRevealedAfterThePasswordIsCorrect() {
        User blocked = new User();
        blocked.setEmail("blocked@example.com");
        blocked.setPassword("hashed");
        blocked.setIsBlocked(true);
        when(userRepository.findByEmail("blocked@example.com")).thenReturn(Optional.of(blocked));

        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        RuntimeException wrongPasswordEx = assertThrows(RuntimeException.class,
                () -> authService.login(request("blocked@example.com", "wrong"), httpRequest));
        assertEquals("Invalid email or password", wrongPasswordEx.getMessage());

        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        RuntimeException blockedEx = assertThrows(RuntimeException.class,
                () -> authService.login(request("blocked@example.com", "correct"), httpRequest));
        assertEquals("This account has been blocked", blockedEx.getMessage());
    }

    @Test
    void unverifiedAccountCannotLogInEvenWithTheCorrectPassword() {
        User unverified = new User();
        unverified.setEmail("pending@example.com");
        unverified.setPassword("hashed");
        unverified.setIsEmailVerified(false);
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request("pending@example.com", "correct"), httpRequest));

        assertEquals("Please verify your email before logging in", ex.getMessage());
        verifyNoInteractions(jwtService, userSessionService);
    }

    @Test
    void correctCredentialsForAVerifiedActiveAccountLogInDirectlyWithoutOtp() {
        User user = new User();
        user.setId(1L);
        user.setEmail("real@example.com");
        user.setPassword("hashed");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setIsEmailVerified(true);
        Role candidateRole = new Role();
        candidateRole.setName("CANDIDATE");
        user.setRoles(java.util.Set.of(candidateRole));
        when(userRepository.findByEmail("real@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct", "hashed")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(userSessionService.issueSession(user, httpRequest)).thenReturn(new IssuedSession(42L, "raw-refresh-token"));

        AuthResponse response = authService.login(request("real@example.com", "correct"), httpRequest);

        assertEquals("jwt-token", response.getToken());
        assertEquals("raw-refresh-token", response.getRefreshToken());
        assertEquals(42L, response.getSessionId());
        assertEquals("real@example.com", response.getEmail());
        verifyNoInteractions(otpService);
    }
}
