package com.job_Portal_Backend.job_portal_backend.otp;

import com.job_Portal_Backend.job_portal_backend.auth.dto.AuthResponse;

public interface OtpService {
    Otp createOtpForEmail(String email);

    boolean verifyOtp(String email, String plainOtp);

    boolean canResendOtp(String email);

    void sendOtpEmail(String email, String plainOtp);

    void cleanupExpiredOtps();
}
