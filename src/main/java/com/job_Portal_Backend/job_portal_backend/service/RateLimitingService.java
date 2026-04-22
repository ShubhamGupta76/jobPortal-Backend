package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import java.time.Duration;

public interface RateLimitingService {

    boolean isAllowed(String key, int maxRequests, Duration window);

    boolean isAllowed(User user, String action, int maxRequests, Duration window);

    boolean isAllowedByIp(String ipAddress, String action, int maxRequests, Duration window);

    void recordRequest(String key);

    void recordRequest(User user, String action);

    void recordRequestByIp(String ipAddress, String action);

    long getRemainingRequests(String key, Duration window);

    long getRemainingRequests(User user, String action, Duration window);

    long getRemainingRequestsByIp(String ipAddress, String action, Duration window);

    Duration getResetTime(String key, Duration window);

    Duration getResetTime(User user, String action, Duration window);

    Duration getResetTimeByIp(String ipAddress, String action, Duration window);

    void resetLimit(String key);

    void resetLimit(User user, String action);

    void resetLimitByIp(String ipAddress, String action);

    // Predefined rate limits for common actions
    boolean checkJobSearchLimit(User user);

    boolean checkApplicationLimit(User user);

    boolean checkLoginLimit(String identifier);

    boolean checkPasswordResetLimit(String email);

    boolean checkFileUploadLimit(User user);

    boolean checkApiCallLimit(String apiKey);

    boolean isAllowed(String key, int maxRequests, long windowSeconds);

    long getRemainingRequests(String key, int maxRequests, Duration window);

    long getRemainingRequests(String key, int maxRequests, long windowSeconds);

    Duration getResetTime(String key);

    boolean allowJobSearch(String userId);

    boolean allowApplicationSubmit(String userId);

    boolean allowJobPost(String userId);

    boolean allowFileUpload(String userId);

    boolean allowApiRequest(String apiKey, String endpoint);

    boolean allowLoginAttempt(String ipAddress);

    boolean allowPasswordReset(String email);

    boolean allowInterviewSchedule(String userId);

    boolean allowNotificationSend(String userId);
}