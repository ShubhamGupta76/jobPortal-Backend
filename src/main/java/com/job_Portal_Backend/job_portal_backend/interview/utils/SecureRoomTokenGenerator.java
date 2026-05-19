package com.job_Portal_Backend.job_portal_backend.interview.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRoomTokenGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public String roomToken() {
        return token(24, "room_");
    }

    public String inviteToken() {
        return token(36, "invite_");
    }

    private String token(int bytes, String prefix) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        return prefix + Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }
}
