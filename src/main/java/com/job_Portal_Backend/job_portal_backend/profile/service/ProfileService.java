package com.job_Portal_Backend.job_portal_backend.profile.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.profile.dto.ProfileResponse;
import com.job_Portal_Backend.job_portal_backend.profile.dto.ProfileUpdateRequest;

import java.io.IOException;

public interface ProfileService {
    ProfileResponse getMyProfile(User user);
    ProfileResponse updateProfile(User user, ProfileUpdateRequest request) throws IOException;
}
