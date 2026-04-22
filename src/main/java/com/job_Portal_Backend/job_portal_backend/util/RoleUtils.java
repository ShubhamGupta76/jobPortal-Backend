package com.job_Portal_Backend.job_portal_backend.util;

import com.job_Portal_Backend.job_portal_backend.entity.Role;

import java.util.Comparator;
import java.util.Set;

public final class RoleUtils {

    private RoleUtils() {
    }

    public static String resolvePrimaryRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalStateException("User has no assigned roles");
        }

        return roles.stream()
                .map(Role::getName)
                .filter(roleName -> roleName != null && !roleName.isBlank())
                .map(String::toUpperCase)
                .min(Comparator.comparingInt(RoleUtils::getRolePriority))
                .orElseThrow(() -> new IllegalStateException("User has no valid roles"));
    }

    private static int getRolePriority(String roleName) {
        return switch (roleName) {
            case "ADMIN" -> 0;
            case "RECRUITER" -> 1;
            case "USER" -> 2;
            default -> 3;
        };
    }
}
