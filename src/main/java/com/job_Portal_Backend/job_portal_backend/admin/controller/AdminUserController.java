package com.job_Portal_Backend.job_portal_backend.admin.controller;

import com.job_Portal_Backend.job_portal_backend.admin.dto.AdminUserDto;
import com.job_Portal_Backend.job_portal_backend.admin.dto.AdminUserStatsResponse;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.SoftDeleteService;
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final SoftDeleteService softDeleteService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminUserStatsResponse>> getStats() {
        AdminUserStatsResponse stats = new AdminUserStatsResponse(
                userRepository.countByRoleNameAndNotDeleted("USER"),
                userRepository.countByRoleNameAndNotDeleted("RECRUITER"),
                userRepository.countByRoleNameAndBlocked("USER"),
                userRepository.countByRoleNameAndBlocked("RECRUITER"));
        return ResponseEntity.ok(new ApiResponse<>(true, "Admin user stats retrieved", stats));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminUserDto>>> getUsers(
            @RequestParam(defaultValue = "RECRUITER") String role,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        String normalizedRole = normalizeRole(role);
        List<User> users = includeDeleted
                ? userRepository.findByRoleName(normalizedRole)
                : userRepository.findByRoleNameAndNotDeleted(normalizedRole);

        List<AdminUserDto> response = users.stream()
                .map(this::toDto)
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(true, "Users retrieved", response));
    }

    @PatchMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<AdminUserDto>> blockUser(@PathVariable Long userId) {
        User user = getManageableUser(userId);
        user.setIsBlocked(true);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "User blocked", toDto(user)));
    }

    @PatchMapping("/{userId}/unblock")
    public ResponseEntity<ApiResponse<AdminUserDto>> unblockUser(@PathVariable Long userId) {
        User user = getManageableUser(userId);
        user.setIsBlocked(false);
        userRepository.save(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "User unblocked", toDto(user)));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        User user = getManageableUser(userId);
        softDeleteService.softDeleteUser(user.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "User account deleted", null));
    }

    private User getManageableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        if ("ADMIN".equals(role)) {
            throw new RuntimeException("Admin accounts cannot be managed from this screen");
        }
        return user;
    }

    private AdminUserDto toDto(User user) {
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        long jobsPosted = "RECRUITER".equals(role) ? jobRepository.countByRecruiterIdAndNotDeleted(user.getId()) : 0;
        long applicationsCount = "USER".equals(role)
                ? applicationRepository.countByUserIdAndNotDeleted(user.getId())
                : applicationRepository.countByJobRecruiterIdAndNotDeleted(user.getId());
        return AdminUserDto.fromUser(user, jobsPosted, applicationsCount);
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "RECRUITER" : role.trim().toUpperCase();
        if ("CANDIDATE".equals(normalized) || "STUDENT".equals(normalized)) {
            return "USER";
        }
        if (!"USER".equals(normalized) && !"RECRUITER".equals(normalized)) {
            throw new RuntimeException("Only recruiter and student users can be managed here");
        }
        return normalized;
    }
}
