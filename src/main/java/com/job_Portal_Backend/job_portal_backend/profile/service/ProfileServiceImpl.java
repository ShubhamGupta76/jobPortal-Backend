package com.job_Portal_Backend.job_portal_backend.profile.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.profile.dto.ProfileResponse;
import com.job_Portal_Backend.job_portal_backend.profile.dto.ProfileUpdateRequest;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final Path resumeDirectory = Paths.get("uploads", "profiles");

    public ProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public ProfileResponse getMyProfile(User user) {
        return toDto(user);
    }

    @Override
    public ProfileResponse updateProfile(User user, ProfileUpdateRequest request) throws IOException {
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getHeadline() != null) user.setHeadline(request.getHeadline());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLocation() != null) user.setLocation(request.getLocation());

        if (request.getResume() != null && !request.getResume().isEmpty()) {
            Files.createDirectories(resumeDirectory);
            String fileName = UUID.randomUUID() + "_" + request.getResume().getOriginalFilename();
            Path filePath = resumeDirectory.resolve(fileName);
            Files.write(filePath, request.getResume().getBytes());
            user.setResumePath(filePath.toString());
        }

        return toDto(userRepository.save(user));
    }

    private ProfileResponse toDto(User user) {
        ProfileResponse dto = new ProfileResponse();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setHeadline(user.getHeadline());
        dto.setBio(user.getBio());
        dto.setLocation(user.getLocation());
        dto.setResumePath(user.getResumePath());
        dto.setRoles(user.getRoles().stream().map(role -> role.getName()).toList());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
