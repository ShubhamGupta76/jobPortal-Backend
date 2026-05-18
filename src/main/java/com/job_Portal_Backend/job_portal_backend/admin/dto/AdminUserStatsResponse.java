package com.job_Portal_Backend.job_portal_backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserStatsResponse {
    private long registeredStudents;
    private long activeRecruiters;
    private long blockedStudents;
    private long blockedRecruiters;
}
