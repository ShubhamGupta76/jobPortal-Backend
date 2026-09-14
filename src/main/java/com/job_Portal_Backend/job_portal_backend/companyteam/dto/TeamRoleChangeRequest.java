package com.job_Portal_Backend.job_portal_backend.companyteam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TeamRoleChangeRequest {

    @NotBlank(message = "Role is required")
    private String role;
}
