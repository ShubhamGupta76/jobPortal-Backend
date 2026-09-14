package com.job_Portal_Backend.job_portal_backend.auth.service;

import com.job_Portal_Backend.job_portal_backend.auth.dto.AuthResponse;
import com.job_Portal_Backend.job_portal_backend.auth.dto.LoginRequest;
import com.job_Portal_Backend.job_portal_backend.auth.dto.RegisterRequest;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.RoleRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.otp.OtpService;
import com.job_Portal_Backend.job_portal_backend.session.service.IssuedSession;
import com.job_Portal_Backend.job_portal_backend.session.service.UserSessionService;
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    // A valid-format BCrypt hash that matches no real password. Comparing against this when the
    // account doesn't exist keeps login's timing indistinguishable from a real wrong-password
    // comparison, so failure timing can't be used to enumerate registered emails.
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$C6UzMDM.H6dfI/f/IKcEeO0RXqoS/xtqTUYVQwWaqzO2wF2GbYlKG";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final UserSessionService userSessionService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, OtpService otpService, UserSessionService userSessionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.userSessionService = userSessionService;
    }

    /**
     * Direct login: validate credentials and issue a JWT/session immediately. OTP is not part of
     * normal login — it remains required only for registration (see sendOtpForRegistration /
     * completeRegistrationAfterOtp), which is unaffected by this method.
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Always run a BCrypt comparison, even when the account doesn't exist, so response
        // timing can't be used to enumerate registered emails. The dummy hash never matches.
        boolean passwordMatches = passwordEncoder.matches(
                request.getPassword(),
                user != null ? user.getPassword() : DUMMY_PASSWORD_HASH);

        if (user == null || Boolean.TRUE.equals(user.getIsDeleted()) || !passwordMatches) {
            // Identical message/status for "no such account" and "wrong password" so a caller
            // can't distinguish account existence from credential validity.
            throw new RuntimeException("Invalid email or password");
        }

        // Blocked/unverified status is only revealed after the password has already been proven
        // correct, so a caller without the right password learns nothing extra either way.
        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new RuntimeException("This account has been blocked");
        }
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new RuntimeException("Please verify your email before logging in");
        }

        String token = jwtService.generateToken(user);
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        IssuedSession session = userSessionService.issueSession(user, httpRequest);

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), role, session.rawRefreshToken(), session.sessionId());
    }

    public AuthResponse completeLoginAfterOtp(String email, HttpServletRequest request) {
        User user = userRepository.findByEmailAndIsEmailVerifiedTrueAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or email not verified"));

        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            throw new RuntimeException("This account has been blocked");
        }

        String token = jwtService.generateToken(user);
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        IssuedSession session = userSessionService.issueSession(user, request);

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), role, session.rawRefreshToken(), session.sessionId());
    }

    @Transactional
    public void sendOtpForRegistration(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Split name into first and last name
        String[] nameParts = request.getName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(request.getPhone());

        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        user.setIsEmailVerified(false); // explicitly

        userRepository.save(user);

        otpService.createOtpForEmail(request.getEmail());
    }

    public AuthResponse completeRegistrationAfterOtp(String email, HttpServletRequest request) {
        User user = userRepository.findByEmailAndIsEmailVerifiedTrueAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or email not verified"));

        String token = jwtService.generateToken(user);
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        IssuedSession session = userSessionService.issueSession(user, request);

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), role, session.rawRefreshToken(), session.sessionId());
    }
}
