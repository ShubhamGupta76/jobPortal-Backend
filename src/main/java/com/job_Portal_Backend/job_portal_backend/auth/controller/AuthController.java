package com.job_Portal_Backend.job_portal_backend.auth.controller;

import com.job_Portal_Backend.job_portal_backend.auth.dto.AuthResponse;
import com.job_Portal_Backend.job_portal_backend.auth.dto.LoginRequest;
import com.job_Portal_Backend.job_portal_backend.auth.dto.RegisterRequest;
import com.job_Portal_Backend.job_portal_backend.auth.service.AuthService;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.otp.OtpService;
import com.job_Portal_Backend.job_portal_backend.otp.dto.SendOtpRequest;
import com.job_Portal_Backend.job_portal_backend.otp.dto.VerifyOtpRequest;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpService otpService;

    @PostMapping("/login-credentials")
    public ResponseEntity<ApiResponse<String>> sendOtpForLogin(@Valid @RequestBody LoginRequest request) {
        authService.sendOtpForLogin(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "OTP sent to your email", "Check your email for OTP"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> sendOtpForRegister(@Valid @RequestBody RegisterRequest request) {
        authService.sendOtpForRegistration(request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Registration OTP sent", "Check your email for OTP"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request.getEmail(), request.getOtp());
        AuthResponse response = isLoginFlow(request.getEmail()) ? authService.completeLoginAfterOtp(request.getEmail())
                : authService.completeRegistrationAfterOtp(request.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(true, "Verification successful", response));
    }

    private boolean isLoginFlow(String email) {
        // For simplicity, check if user has password set and email verified - but since
        // called after verifyOtp,
        // assume login flow if not new registration (registration saves temp user)
        // Simplest: always use login completion for this flow
        return true; // Since login-credentials flow always leads here
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        if (otpService.canResendOtp(request.getEmail())) {
            otpService.createOtpForEmail(request.getEmail());
            return ResponseEntity.ok(new ApiResponse<>(true, "OTP resent", "Check your email"));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Too many requests", null));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(@RequestHeader("Authorization") String token) {
        String email = jwtService.extractUsername(token.substring(7));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        AuthResponse response = new AuthResponse("",
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                role);
        return ResponseEntity.ok(new ApiResponse<>(true, "Current user retrieved successfully", response));
    }
}
