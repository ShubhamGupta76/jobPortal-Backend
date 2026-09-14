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
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyTeamInvitationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyTeamMemberRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.EmailService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyTeamServiceTest {

    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final CompanyTeamMemberRepository teamMemberRepository = mock(CompanyTeamMemberRepository.class);
    private final CompanyTeamInvitationRepository invitationRepository = mock(CompanyTeamInvitationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final EmailService emailService = mock(EmailService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final CompanyTeamService service = new CompanyTeamService(
            companyRepository, teamMemberRepository, invitationRepository, userRepository,
            notificationService, emailService, auditLogService);

    @Test
    void nonMemberCannotViewTeam() {
        User owner = user(1L, "owner@example.com");
        User stranger = user(2L, "stranger@example.com");
        Company company = company(10L, owner);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getTeam(10L, stranger));
    }

    @Test
    void ownerViewBackfillsOwnerMembershipLazily() {
        User owner = user(1L, "owner@example.com");
        Company company = company(10L, owner);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(0L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        when(teamMemberRepository.findByCompanyIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(invitationRepository.findByCompanyIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

        CompanyTeamResponse response = service.getTeam(10L, owner);

        assertEquals("OWNER", response.getViewerRole());
        verify(teamMemberRepository).save(argThat(m -> m.getRole() == TeamRole.OWNER && m.getUser().getId().equals(1L)));
    }

    @Test
    void nonManagerCannotInvite() {
        User owner = user(1L, "owner@example.com");
        User viewer = user(3L, "viewer@example.com");
        Company company = company(10L, owner);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 3L))
                .thenReturn(Optional.of(member(company, viewer, TeamRole.VIEWER)));

        TeamInviteRequest request = new TeamInviteRequest();
        request.setEmail("new@example.com");
        request.setRole("RECRUITER");

        assertThrows(ResponseStatusException.class, () -> service.invite(10L, request, viewer));
        verifyNoInteractions(invitationRepository);
    }

    @Test
    void cannotInviteAsOwner() {
        User owner = user(1L, "owner@example.com");
        Company company = company(10L, owner);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));

        TeamInviteRequest request = new TeamInviteRequest();
        request.setEmail("new@example.com");
        request.setRole("OWNER");

        assertThrows(ResponseStatusException.class, () -> service.invite(10L, request, owner));
    }

    @Test
    void managerCanInviteAndDuplicatePendingInviteIsRejected() {
        User owner = user(1L, "owner@example.com");
        Company company = company(10L, owner);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(member(company, owner, TeamRole.OWNER)));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(invitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndStatus(10L, "new@example.com", InvitationStatus.PENDING))
                .thenReturn(false);
        when(invitationRepository.save(any(CompanyTeamInvitation.class))).thenAnswer(invocation -> {
            CompanyTeamInvitation inv = invocation.getArgument(0);
            inv.setId(50L);
            return inv;
        });

        TeamInviteRequest request = new TeamInviteRequest();
        request.setEmail("new@example.com");
        request.setRole("RECRUITER");

        CompanyTeamInvitationDto dto = service.invite(10L, request, owner);
        assertEquals("PENDING", dto.getStatus());
        assertNotNull(dto.getToken());

        when(invitationRepository.existsByCompanyIdAndEmailIgnoreCaseAndStatus(10L, "new@example.com", InvitationStatus.PENDING))
                .thenReturn(true);
        assertThrows(ResponseStatusException.class, () -> service.invite(10L, request, owner));
    }

    @Test
    void acceptInvitationRejectsMismatchedEmail() {
        User invitedByOwner = user(1L, "owner@example.com");
        Company company = company(10L, invitedByOwner);
        CompanyTeamInvitation invitation = invitation(company, "invitee@example.com", TeamRole.RECRUITER, invitedByOwner);

        when(invitationRepository.findByToken("tok-1")).thenReturn(Optional.of(invitation));

        User wrongUser = user(5L, "someoneelse@example.com");
        assertThrows(ResponseStatusException.class, () -> service.acceptInvitation("tok-1", wrongUser));
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitationCreatesMembershipForMatchingEmail() {
        User invitedByOwner = user(1L, "owner@example.com");
        Company company = company(10L, invitedByOwner);
        CompanyTeamInvitation invitation = invitation(company, "invitee@example.com", TeamRole.RECRUITER, invitedByOwner);

        when(invitationRepository.findByToken("tok-1")).thenReturn(Optional.of(invitation));
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 7L)).thenReturn(Optional.empty());
        when(teamMemberRepository.save(any(CompanyTeamMember.class))).thenAnswer(invocation -> {
            CompanyTeamMember m = invocation.getArgument(0);
            m.setId(99L);
            return m;
        });

        User invitee = user(7L, "invitee@example.com");
        CompanyTeamMemberDto dto = service.acceptInvitation("tok-1", invitee);

        assertEquals("RECRUITER", dto.getRole());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
        verify(notificationService).sendNotificationToUser(eq(invitedByOwner), anyString(), anyString(), anyString());
    }

    @Test
    void cannotRemoveTheLastOwner() {
        User owner = user(1L, "owner@example.com");
        Company company = company(10L, owner);
        CompanyTeamMember ownerMembership = member(company, owner, TeamRole.OWNER);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(teamMemberRepository.countByCompanyIdAndRole(10L, TeamRole.OWNER)).thenReturn(1L);

        assertThrows(ResponseStatusException.class, () -> service.removeMember(10L, 1L, owner));
        verify(teamMemberRepository, never()).delete(any());
    }

    @Test
    void onlyOwnerCanChangeRoles() {
        User owner = user(1L, "owner@example.com");
        User admin = user(2L, "admin@example.com");
        Company company = company(10L, owner);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(teamMemberRepository.countByCompanyId(10L)).thenReturn(1L);
        when(teamMemberRepository.findByCompanyIdAndUserId(10L, 2L))
                .thenReturn(Optional.of(member(company, admin, TeamRole.ADMIN)));

        assertThrows(ResponseStatusException.class, () -> service.changeRole(10L, 3L, "VIEWER", admin));
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        return user;
    }

    private Company company(Long id, User owner) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme Inc");
        company.setOwner(owner);
        return company;
    }

    private CompanyTeamMember member(Company company, User user, TeamRole role) {
        CompanyTeamMember member = new CompanyTeamMember();
        member.setCompany(company);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private CompanyTeamInvitation invitation(Company company, String email, TeamRole role, User invitedBy) {
        CompanyTeamInvitation invitation = new CompanyTeamInvitation();
        invitation.setId(50L);
        invitation.setCompany(company);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setToken("tok-1");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setInvitedBy(invitedBy);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        return invitation;
    }
}
