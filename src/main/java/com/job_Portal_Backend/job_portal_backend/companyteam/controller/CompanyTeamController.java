package com.job_Portal_Backend.job_portal_backend.companyteam.controller;

import com.job_Portal_Backend.job_portal_backend.companyteam.dto.CompanyTeamInvitationDto;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.CompanyTeamMemberDto;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.CompanyTeamResponse;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.TeamInviteRequest;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.TeamRoleChangeRequest;
import com.job_Portal_Backend.job_portal_backend.companyteam.service.CompanyTeamService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class CompanyTeamController {

    private final CompanyTeamService companyTeamService;

    public CompanyTeamController(CompanyTeamService companyTeamService) {
        this.companyTeamService = companyTeamService;
    }

    @GetMapping("/companies/{companyId}/team")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyTeamResponse>> getTeam(
            @PathVariable Long companyId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Team retrieved successfully",
                companyTeamService.getTeam(companyId, user)));
    }

    @PostMapping("/companies/{companyId}/team/invitations")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyTeamInvitationDto>> invite(
            @PathVariable Long companyId,
            @Valid @RequestBody TeamInviteRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Invitation sent successfully",
                companyTeamService.invite(companyId, request, user)));
    }

    @DeleteMapping("/companies/{companyId}/team/invitations/{invitationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> revokeInvitation(
            @PathVariable Long companyId,
            @PathVariable Long invitationId,
            @AuthenticationPrincipal User user) {
        companyTeamService.revokeInvitation(companyId, invitationId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Invitation cancelled", null));
    }

    @DeleteMapping("/companies/{companyId}/team/{userId}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {
        companyTeamService.removeMember(companyId, userId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Team member removed", null));
    }

    @PatchMapping("/companies/{companyId}/team/{userId}/role")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyTeamMemberDto>> changeRole(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @Valid @RequestBody TeamRoleChangeRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Role updated successfully",
                companyTeamService.changeRole(companyId, userId, request.getRole(), user)));
    }

    @PostMapping("/team/invitations/{token}/accept")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyTeamMemberDto>> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Invitation accepted",
                companyTeamService.acceptInvitation(token, user)));
    }
}
