package com.job_Portal_Backend.job_portal_backend.profile.controller;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.profile.dto.ProfileResponse;
import com.job_Portal_Backend.job_portal_backend.profile.dto.ProfileUpdateRequest;
import com.job_Portal_Backend.job_portal_backend.profile.service.ProfileService;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    private final ProfileService profileService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public ProfileController(ProfileService profileService, JwtService jwtService, UserRepository userRepository) {
        this.profileService = profileService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@RequestHeader("Authorization") String token) {
        User user = resolveUser(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile retrieved successfully", profileService.getMyProfile(user)));
    }

    @PutMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @ModelAttribute ProfileUpdateRequest request,
            @RequestHeader("Authorization") String token
    ) throws IOException {
        User user = resolveUser(token);
        return ResponseEntity.ok(new ApiResponse<>(true, "Profile updated successfully", profileService.updateProfile(user, request)));
    }

    private User resolveUser(String token) {
        String email = jwtService.extractUsername(token.substring(7));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
