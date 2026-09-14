package com.job_Portal_Backend.job_portal_backend.companyteam.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyTeamMemberDto {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String role;
    private LocalDateTime joinedAt;
}
