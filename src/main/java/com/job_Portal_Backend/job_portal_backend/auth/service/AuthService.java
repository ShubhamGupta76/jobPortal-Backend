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
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService, OtpService otpService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
    }

    public void sendOtpForLogin(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        otpService.createOtpForEmail(request.getEmail());
    }

    public AuthResponse completeLoginAfterOtp(String email) {
        User user = userRepository.findByEmailAndIsEmailVerifiedTrueAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or email not verified"));

        String token = jwtService.generateToken(user);
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), role);
    }

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

    public AuthResponse completeRegistrationAfterOtp(String email) {
        User user = userRepository.findByEmailAndIsEmailVerifiedTrueAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or email not verified"));

        String token = jwtService.generateToken(user);
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(), role);
    }
}
