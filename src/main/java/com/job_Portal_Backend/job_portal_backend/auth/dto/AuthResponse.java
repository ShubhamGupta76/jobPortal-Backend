package com.job_Portal_Backend.job_portal_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    // Opaque refresh token, additive to the existing access token; null where no session is
    // issued (e.g. GET /me). Never a JWT and never logged.
    private String refreshToken;
    private Long sessionId;
}