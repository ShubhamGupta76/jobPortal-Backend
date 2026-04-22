package com.job_Portal_Backend.job_portal_backend.controller;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files/resume")
public class ResumeController {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    public ResumeController(JwtService jwtService, UserRepository userRepository, ApplicationRepository applicationRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('USER','RECRUITER')")
    public ResponseEntity<Resource> getResume(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestHeader("Authorization") String token) throws IOException {
        User requester = resolveUser(token);
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String resumePath = resolveResumePath(requester, targetUser);
        if (resumePath == null || resumePath.isBlank()) {
            throw new ResourceNotFoundException("Resume not found");
        }

        Path filePath = Paths.get(resumePath);
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Resume file not found on disk");
        }

        String contentType = Files.probeContentType(filePath);
        String filename = filePath.getFileName().toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/pdf"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (download ? "attachment" : "inline") + "; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(Files.readAllBytes(filePath)));
    }

    private User resolveUser(String token) {
        String email = jwtService.extractUsername(token.substring(7));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String resolveResumePath(User requester, User targetUser) {
        if (requester.getId().equals(targetUser.getId())) {
            return findPreferredResumePath(targetUser.getId(), null, targetUser.getResumePath());
        }

        boolean isRecruiter = requester.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RECRUITER".equals(authority.getAuthority()));
        if (!isRecruiter) {
            throw new RuntimeException("Unauthorized to view this resume");
        }

        return findPreferredResumePath(targetUser.getId(), requester.getId(), targetUser.getResumePath());
    }

    private String findPreferredResumePath(Long targetUserId, Long recruiterId, String profileResumePath) {
        if (profileResumePath != null && !profileResumePath.isBlank()) {
            return profileResumePath;
        }

        List<Application> applications = recruiterId != null
                ? applicationRepository.findResumeApplicationsByUserIdAndRecruiterId(targetUserId, recruiterId)
                : applicationRepository.findResumeApplicationsByUserId(targetUserId);

        return applications.stream()
                .map(Application::getResumePath)
                .filter(path -> path != null && !path.isBlank())
                .findFirst()
                .orElse(null);
    }
}
