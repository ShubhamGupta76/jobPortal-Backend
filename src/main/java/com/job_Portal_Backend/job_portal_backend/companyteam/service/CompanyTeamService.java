package com.job_Portal_Backend.job_portal_backend.companyteam.service;

import com.job_Portal_Backend.job_portal_backend.companyteam.dto.CompanyTeamInvitationDto;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.CompanyTeamMemberDto;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.CompanyTeamResponse;
import com.job_Portal_Backend.job_portal_backend.companyteam.dto.TeamInviteRequest;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamInvitation;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamInvitation.InvitationStatus;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamMember;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamMember.TeamRole;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyTeamInvitationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyTeamMemberRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.EmailService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CompanyTeamService {

    private static final Set<TeamRole> MANAGE_ROLES = Set.of(TeamRole.OWNER, TeamRole.ADMIN);
    private static final int INVITATION_VALIDITY_DAYS = 7;

    private final CompanyRepository companyRepository;
    private final CompanyTeamMemberRepository teamMemberRepository;
    private final CompanyTeamInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public CompanyTeamService(CompanyRepository companyRepository,
            CompanyTeamMemberRepository teamMemberRepository,
            CompanyTeamInvitationRepository invitationRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            EmailService emailService,
            AuditLogService auditLogService) {
        this.companyRepository = companyRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CompanyTeamResponse getTeam(Long companyId, User actor) {
        Company company = requireCompany(companyId);
        TeamRole viewerRole = ensureOwnerMembershipAndResolveRole(company, actor);
        if (viewerRole == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this company's team.");
        }

        CompanyTeamResponse response = new CompanyTeamResponse();
        response.setMembers(teamMemberRepository.findByCompanyIdOrderByCreatedAtAsc(companyId).stream()
                .map(this::toMemberDto)
                .toList());
        response.setPendingInvitations(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .filter(invitation -> invitation.getStatus() == InvitationStatus.PENDING)
                .map(this::toInvitationDto)
                .toList());
        response.setViewerRole(viewerRole.name());
        return response;
    }

    @Transactional
    public CompanyTeamInvitationDto invite(Long companyId, TeamInviteRequest request, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, MANAGE_ROLES);

        TeamRole role = parseInvitableRole(request.getRole());
        String email = request.getEmail().trim().toLowerCase();

        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()
                && teamMemberRepository.findByCompanyIdAndUserId(companyId, existingUser.get().getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This person is already on the team.");
        }
        if (invitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndStatus(companyId, email, InvitationStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An invitation is already pending for this email.");
        }

        CompanyTeamInvitation invitation = new CompanyTeamInvitation();
        invitation.setCompany(company);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(actor);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(INVITATION_VALIDITY_DAYS));
        CompanyTeamInvitation saved = invitationRepository.save(invitation);

        auditLogService.logAction("CompanyTeamInvitation", saved.getId(), "CREATE", actor,
                "Invited " + email + " to " + company.getName() + " as " + role, null, null);

        if (existingUser.isPresent()) {
            notificationService.sendNotificationToUser(existingUser.get(),
                    "Team invitation",
                    "You have been invited to join " + company.getName() + " as " + formatLabel(role.name()) + ".",
                    "TEAM_INVITATION", "{\"token\":\"" + saved.getToken() + "\"}");
        }
        try {
            emailService.sendBulkEmail(new String[] { email },
                    "You're invited to join " + company.getName() + " on JobPortal",
                    "You have been invited to join " + company.getName() + " as " + formatLabel(role.name())
                            + ". Sign in and open your invitation to accept.");
        } catch (RuntimeException emailFailure) {
            // Email delivery is best-effort; the invitation and its shareable link remain valid regardless.
        }

        return toInvitationDto(saved);
    }

    @Transactional
    public void revokeInvitation(Long companyId, Long invitationId, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, MANAGE_ROLES);

        CompanyTeamInvitation invitation = invitationRepository.findById(invitationId)
                .filter(item -> item.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending invitations can be cancelled.");
        }
        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
        auditLogService.logAction("CompanyTeamInvitation", invitation.getId(), "REVOKE", actor,
                "Revoked invitation for " + invitation.getEmail(), null, null);
    }

    @Transactional
    public CompanyTeamMemberDto acceptInvitation(String token, User actor) {
        CompanyTeamInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() == InvitationStatus.PENDING && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This invitation is " + invitation.getStatus().name().toLowerCase() + " and can no longer be accepted.");
        }
        if (!invitation.getEmail().equalsIgnoreCase(actor.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This invitation was sent to a different email address.");
        }

        Company company = invitation.getCompany();
        CompanyTeamMember membership = teamMemberRepository.findByCompanyIdAndUserId(company.getId(), actor.getId())
                .orElseGet(CompanyTeamMember::new);
        membership.setCompany(company);
        membership.setUser(actor);
        membership.setRole(invitation.getRole());
        CompanyTeamMember saved = teamMemberRepository.save(membership);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedBy(actor);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        auditLogService.logAction("CompanyTeamMember", saved.getId(), "CREATE", actor,
                actor.getEmail() + " joined " + company.getName() + " as " + invitation.getRole(), null, null);
        notificationService.sendNotificationToUser(invitation.getInvitedBy(),
                "Invitation accepted",
                actor.getEmail() + " accepted your invitation to join " + company.getName() + ".",
                "TEAM_INVITATION_ACCEPTED");

        return toMemberDto(saved);
    }

    @Transactional
    public void removeMember(Long companyId, Long userId, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, MANAGE_ROLES);

        CompanyTeamMember target = teamMemberRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found"));

        if (target.getRole() == TeamRole.OWNER && teamMemberRepository.countByCompanyIdAndRole(companyId, TeamRole.OWNER) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A company must always have at least one owner.");
        }

        teamMemberRepository.delete(target);
        auditLogService.logAction("CompanyTeamMember", target.getId(), "DELETE", actor,
                "Removed " + target.getUser().getEmail() + " from " + company.getName(), null, null);
    }

    @Transactional
    public CompanyTeamMemberDto changeRole(Long companyId, Long userId, String roleValue, User actor) {
        Company company = requireCompany(companyId);
        requireActorRole(company, actor, Set.of(TeamRole.OWNER));

        TeamRole newRole = parseInvitableRole(roleValue);
        CompanyTeamMember target = teamMemberRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Team member not found"));

        if (target.getRole() == TeamRole.OWNER && teamMemberRepository.countByCompanyIdAndRole(companyId, TeamRole.OWNER) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reassign another owner before changing this member's role; a company must always have at least one owner.");
        }

        TeamRole previousRole = target.getRole();
        target.setRole(newRole);
        CompanyTeamMember saved = teamMemberRepository.save(target);

        auditLogService.logAction("CompanyTeamMember", saved.getId(), "UPDATE", actor,
                "Changed " + target.getUser().getEmail() + " from " + previousRole + " to " + newRole
                        + " in " + company.getName(), null, null);
        return toMemberDto(saved);
    }

    // --- helpers -------------------------------------------------------

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
    }

    /**
     * Lazily backfills an OWNER membership row for companies created before team management
     * existed, then resolves the acting user's role (or null if they have no membership at all).
     */
    private TeamRole ensureOwnerMembershipAndResolveRole(Company company, User actor) {
        if (company.getOwner() != null && teamMemberRepository.countByCompanyId(company.getId()) == 0) {
            CompanyTeamMember ownerMembership = new CompanyTeamMember();
            ownerMembership.setCompany(company);
            ownerMembership.setUser(company.getOwner());
            ownerMembership.setRole(TeamRole.OWNER);
            teamMemberRepository.save(ownerMembership);
        }
        return teamMemberRepository.findByCompanyIdAndUserId(company.getId(), actor.getId())
                .map(CompanyTeamMember::getRole)
                .orElse(company.getOwner() != null && company.getOwner().getId().equals(actor.getId()) ? TeamRole.OWNER : null);
    }

    private TeamRole requireActorRole(Company company, User actor, Set<TeamRole> allowed) {
        TeamRole role = ensureOwnerMembershipAndResolveRole(company, actor);
        if (role == null || !allowed.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have permission to manage this company's team.");
        }
        return role;
    }

    private TeamRole parseInvitableRole(String value) {
        TeamRole role = parseRole(value);
        if (role == TeamRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ownership cannot be assigned through invitations or role changes.");
        }
        return role;
    }

    private TeamRole parseRole(String value) {
        try {
            return TeamRole.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team role: " + value);
        }
    }

    private CompanyTeamMemberDto toMemberDto(CompanyTeamMember member) {
        CompanyTeamMemberDto dto = new CompanyTeamMemberDto();
        dto.setId(member.getId());
        dto.setUserId(member.getUser().getId());
        dto.setName(fullName(member.getUser()));
        dto.setEmail(member.getUser().getEmail());
        dto.setRole(member.getRole().name());
        dto.setJoinedAt(member.getCreatedAt());
        return dto;
    }

    private CompanyTeamInvitationDto toInvitationDto(CompanyTeamInvitation invitation) {
        CompanyTeamInvitationDto dto = new CompanyTeamInvitationDto();
        dto.setId(invitation.getId());
        dto.setEmail(invitation.getEmail());
        dto.setRole(invitation.getRole().name());
        dto.setStatus(invitation.getStatus().name());
        dto.setInvitedByName(fullName(invitation.getInvitedBy()));
        dto.setToken(invitation.getToken());
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setExpiresAt(invitation.getExpiresAt());
        return dto;
    }

    private String fullName(User user) {
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String formatLabel(String value) {
        String lower = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
