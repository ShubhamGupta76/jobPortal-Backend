package com.job_Portal_Backend.job_portal_backend.companyteam.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompanyTeamResponse {
    private List<CompanyTeamMemberDto> members;
    private List<CompanyTeamInvitationDto> pendingInvitations;
    private String viewerRole;
}
