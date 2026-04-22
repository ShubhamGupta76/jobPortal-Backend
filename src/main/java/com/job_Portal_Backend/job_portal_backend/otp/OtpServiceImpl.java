package com.job_Portal_Backend.job_portal_backend.otp;

import com.job_Portal_Backend.job_portal_backend.otp.Otp;
import com.job_Portal_Backend.job_portal_backend.otp.OtpRepository;
import com.job_Portal_Backend.job_portal_backend.service.EmailService;
import com.job_Portal_Backend.job_portal_backend.service.RateLimitingService;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimitingService rateLimitingService;
    private final UserRepository userRepository;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Override
    public Otp createOtpForEmail(String email) {
        // Delete old expired OTPS
        cleanupExpiredOtps();

        // Check rate limit for resend
        if (!canResendOtp(email)) {
            throw new RuntimeException("Too many OTP requests. Try again later.");
        }

        // Generate new OTP
        String plainOtp = generateRandomOtp();
        Otp otp = new Otp();
        otp.setEmail(email);
        otp.setOtpHash(passwordEncoder.encode(plainOtp));
        otp.setAttempts(0);
        otp.setVerified(false);
        otp.setExpiryTime(LocalDateTime.now().plusMinutes(otpExpiryMinutes));

        otp = otpRepository.save(otp);

        // Send email
        sendOtpEmail(email, plainOtp);

        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String plainOtp) {
        LocalDateTime now = LocalDateTime.now();
        Otp otp = otpRepository.findByEmailAndVerifiedFalseAndExpiryTimeAfter(email, now)
                .orElseThrow(() -> new RuntimeException("No valid OTP found for this email"));

        if (otp.getAttempts() >= maxAttempts) {
            throw new RuntimeException("Max OTP attempts exceeded");
        }

        if (!passwordEncoder.matches(plainOtp, otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpRepository.save(otp);
            throw new RuntimeException("Invalid OTP");
        }

        if (now.isAfter(otp.getExpiryTime())) {
            throw new RuntimeException("OTP expired");
        }

        // Update user email verified status
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for email: " + email));
        user.setIsEmailVerified(true);
        userRepository.save(user);

        otp.setVerified(true);
        otpRepository.save(otp);
        return true;
    }

    @Override
    public boolean canResendOtp(String email) {
        return rateLimitingService.isAllowed(email, 3, Duration.ofSeconds(30));
    }

    @Override
    public void sendOtpEmail(String email, String plainOtp) {
        emailService.sendOtpEmail(email, plainOtp);
    }

    @Override
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
    }

    private String generateRandomOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
