package com.job_Portal_Backend.job_portal_backend.profile.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String headline;
    private String bio;
    private String location;
    private MultipartFile resume;
}
