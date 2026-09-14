package com.job_Portal_Backend.job_portal_backend.companyteam.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyTeamInvitationDto {
    private Long id;
    private String email;
    private String role;
    private String status;
    private String invitedByName;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
