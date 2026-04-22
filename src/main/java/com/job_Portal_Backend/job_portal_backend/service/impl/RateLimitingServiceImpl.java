package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private String buildKey(String identifier, String action) {
        return RATE_LIMIT_PREFIX + identifier + ":" + action;
    }

    private String buildKey(String key) {
        return RATE_LIMIT_PREFIX + key;
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = buildKey(key);
        Long currentCount = redisTemplate.opsForValue().increment(redisKey);

        if (currentCount == 1) {
            redisTemplate.expire(redisKey, window);
        }

        return currentCount <= maxRequests;
    }

    @Override
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        return isAllowed(key, maxRequests, Duration.ofSeconds(windowSeconds));
    }

    @Override
    public boolean isAllowed(User user, String action, int maxRequests, Duration window) {
        return isAllowed(buildKey("user:" + user.getId(), action), maxRequests, window);
    }

    @Override
    public boolean isAllowedByIp(String ipAddress, String action, int maxRequests, Duration window) {
        return isAllowed(buildKey("ip:" + ipAddress, action), maxRequests, window);
    }

    @Override
    public void recordRequest(String key) {
        String redisKey = buildKey(key);
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        }
    }

    @Override
    public void recordRequest(User user, String action) {
        recordRequest(buildKey("user:" + user.getId(), action));
    }

    @Override
    public void recordRequestByIp(String ipAddress, String action) {
        recordRequest(buildKey("ip:" + ipAddress, action));
    }

    @Override
    public long getRemainingRequests(String key, int maxRequests, Duration window) {
        String redisKey = buildKey(key);
        String countStr = redisTemplate.opsForValue().get(redisKey);

        if (countStr == null) {
            return maxRequests;
        }

        long currentCount = Long.parseLong(countStr);
        return Math.max(0, maxRequests - currentCount);
    }

    @Override
    public long getRemainingRequests(String key, int maxRequests, long windowSeconds) {
        return getRemainingRequests(key, maxRequests, Duration.ofSeconds(windowSeconds));
    }

    @Override
    public long getRemainingRequests(String key, Duration window) {
        String redisKey = buildKey(key);
        String countStr = redisTemplate.opsForValue().get(redisKey);
        return countStr == null ? 0L : Long.parseLong(countStr);
    }

    @Override
    public long getRemainingRequests(User user, String action, Duration window) {
        return getRemainingRequests(buildKey("user:" + user.getId(), action), window);
    }

    @Override
    public long getRemainingRequestsByIp(String ipAddress, String action, Duration window) {
        return getRemainingRequests(buildKey("ip:" + ipAddress, action), window);
    }

    @Override
    public Duration getResetTime(String key, Duration window) {
        String redisKey = buildKey(key);
        Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        return ttl == null || ttl <= 0 ? Duration.ZERO : Duration.ofSeconds(ttl);
    }

    @Override
    public Duration getResetTime(User user, String action, Duration window) {
        return getResetTime(buildKey("user:" + user.getId(), action), window);
    }

    @Override
    public Duration getResetTimeByIp(String ipAddress, String action, Duration window) {
        return getResetTime(buildKey("ip:" + ipAddress, action), window);
    }

    @Override
    public void resetLimit(String key) {
        redisTemplate.delete(buildKey(key));
    }

    @Override
    public void resetLimit(User user, String action) {
        resetLimit(buildKey("user:" + user.getId(), action));
    }

    @Override
    public void resetLimitByIp(String ipAddress, String action) {
        resetLimit(buildKey("ip:" + ipAddress, action));
    }

    @Override
    public boolean checkJobSearchLimit(User user) {
        return isAllowed(user, "job_search", 100, Duration.ofHours(1));
    }

    @Override
    public boolean checkApplicationLimit(User user) {
        return isAllowed(user, "application_submit", 50, Duration.ofDays(1));
    }

    @Override
    public boolean checkLoginLimit(String identifier) {
        return isAllowedByIp(identifier, "login", 5, Duration.ofMinutes(15));
    }

    @Override
    public boolean checkPasswordResetLimit(String email) {
        return isAllowedByIp(email, "password_reset", 3, Duration.ofHours(1));
    }

    @Override
    public boolean checkFileUploadLimit(User user) {
        return isAllowed(user, "file_upload", 10, Duration.ofHours(1));
    }

    @Override
    public boolean checkApiCallLimit(String apiKey) {
        return isAllowed(buildKey("api:" + apiKey), 1000, Duration.ofMinutes(1));
    }

    @Override
    public Duration getResetTime(String key) {
        return getResetTime(key, Duration.ZERO);
    }

    @Override
    public boolean allowJobSearch(String userId) {
        // Allow 100 job searches per hour per user
        return isAllowed("job_search:" + userId, 100, Duration.ofHours(1));
    }

    @Override
    public boolean allowApplicationSubmit(String userId) {
        // Allow 50 applications per day per user
        return isAllowed("application_submit:" + userId, 50, Duration.ofDays(1));
    }

    @Override
    public boolean allowJobPost(String userId) {
        // Allow 20 job posts per day per recruiter
        return isAllowed("job_post:" + userId, 20, Duration.ofDays(1));
    }

    @Override
    public boolean allowFileUpload(String userId) {
        // Allow 10 file uploads per hour per user
        return isAllowed("file_upload:" + userId, 10, Duration.ofHours(1));
    }

    @Override
    public boolean allowApiRequest(String apiKey, String endpoint) {
        // Allow 1000 requests per minute per API key per endpoint
        return isAllowed("api:" + apiKey + ":" + endpoint, 1000, Duration.ofMinutes(1));
    }

    @Override
    public boolean allowLoginAttempt(String ipAddress) {
        // Allow 5 login attempts per 15 minutes per IP
        return isAllowed("login:" + ipAddress, 5, Duration.ofMinutes(15));
    }

    @Override
    public boolean allowPasswordReset(String email) {
        // Allow 3 password reset requests per hour per email
        return isAllowed("password_reset:" + email, 3, Duration.ofHours(1));
    }

    @Override
    public boolean allowInterviewSchedule(String userId) {
        // Allow 10 interview schedules per day per user
        return isAllowed("interview_schedule:" + userId, 10, Duration.ofDays(1));
    }

    @Override
    public boolean allowNotificationSend(String userId) {
        // Allow 100 notifications per hour per user
        return isAllowed("notification:" + userId, 100, Duration.ofHours(1));
    }
}