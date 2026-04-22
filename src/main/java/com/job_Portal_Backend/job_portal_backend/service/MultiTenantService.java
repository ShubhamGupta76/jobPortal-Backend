package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;

public interface MultiTenantService {

    String getCurrentTenantId();

    void setCurrentTenantId(String tenantId);

    boolean isTenantValid(String tenantId);

    User getCurrentUser();

    void setCurrentUser(User user);

    boolean hasPermission(String permission);

    boolean hasRole(String role);

    boolean isAdmin();

    boolean isRecruiter();

    boolean isCandidate();

    String getTenantSchemaName(String tenantId);

    void validateTenantAccess(String tenantId, User user);

    void initializeTenant(String tenantId, String tenantName, User owner);

    void deactivateTenant(String tenantId);

    void activateTenant(String tenantId);

    boolean isTenantActive(String tenantId);

    long getTenantUserCount(String tenantId);

    long getTenantJobCount(String tenantId);

    long getTenantApplicationCount(String tenantId);
}