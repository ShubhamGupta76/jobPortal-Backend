package com.job_Portal_Backend.job_portal_backend.profile.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String headline;
    private String bio;
    private String location;
    private String resumePath;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
