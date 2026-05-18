package com.job_Portal_Backend.job_portal_backend.admin.dto;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String headline;
    private String role;
    private Boolean emailVerified;
    private Boolean blocked;
    private Boolean deleted;
    private Long jobsPosted;
    private Long applicationsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AdminUserDto fromUser(User user, long jobsPosted, long applicationsCount) {
        return new AdminUserDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getHeadline(),
                RoleUtils.resolvePrimaryRole(user.getRoles()),
                user.getIsEmailVerified(),
                user.getIsBlocked(),
                user.getIsDeleted(),
                jobsPosted,
                applicationsCount,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
