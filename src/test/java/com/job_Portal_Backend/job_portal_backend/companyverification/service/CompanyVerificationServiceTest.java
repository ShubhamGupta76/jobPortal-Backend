package com.job_Portal_Backend.job_portal_backend.companyverification.service;

import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationRequest;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationResponse;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification.VerificationStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationDocumentRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationHistoryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.FileUploadService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CompanyVerificationServiceTest {

    private final CompanyVerificationRepository verificationRepository = mock(CompanyVerificationRepository.class);
    private final CompanyVerificationDocumentRepository documentRepository = mock(CompanyVerificationDocumentRepository.class);
    private final CompanyVerificationHistoryRepository historyRepository = mock(CompanyVerificationHistoryRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FileUploadService fileUploadService = mock(FileUploadService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final CompanyVerificationService service = new CompanyVerificationService(
            verificationRepository, documentRepository, historyRepository, companyRepository,
            userRepository, fileUploadService, auditLogService, notificationService);

    @Test
    void nonOwnerCannotSubmitVerification() {
        User owner = user(1L, "USER");
        User stranger = user(2L, "RECRUITER");
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));

        CompanyVerificationRequest request = new CompanyVerificationRequest();
        request.setLegalName("Acme Inc");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(10L, request, stranger));
        assertEquals(403, ex.getStatusCode().value());
        verifyNoInteractions(notificationService);
    }

    @Test
    void ownerSubmissionCreatesPendingRecordAndNotifiesAdmins() {
        User owner = user(1L, "RECRUITER");
        Company company = company(10L, owner);
        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(verificationRepository.findByCompanyId(10L)).thenReturn(Optional.empty());
        when(verificationRepository.save(any(CompanyVerification.class))).thenAnswer(invocation -> {
            CompanyVerification v = invocation.getArgument(0);
            v.setId(100L);
            return v;
        });
        when(userRepository.findByRoleNameAndNotDeleted("ADMIN")).thenReturn(List.of(user(9L, "ADMIN")));

        CompanyVerificationRequest request = new CompanyVerificationRequest();
        request.setLegalName("Acme Inc");

        CompanyVerificationResponse response = service.submit(10L, request, owner);

        assertEquals("PENDING", response.getStatus());
        verify(historyRepository).save(argThat(h -> h.getStatus() == VerificationStatus.PENDING));
        verify(notificationService).sendNotificationToUsers(anyList(), anyString(), anyString(), eq("COMPANY_VERIFICATION_SUBMITTED"));
    }

    @Test
    void cannotResubmitWhilePending() {
        User owner = user(1L, "RECRUITER");
        Company company = company(10L, owner);
        CompanyVerification existing = new CompanyVerification();
        existing.setId(100L);
        existing.setCompany(company);
        existing.setStatus(VerificationStatus.PENDING);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        when(verificationRepository.findByCompanyId(10L)).thenReturn(Optional.of(existing));

        CompanyVerificationRequest request = new CompanyVerificationRequest();
        request.setLegalName("Acme Inc");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submit(10L, request, owner));
        assertEquals(409, ex.getStatusCode().value());
        verify(verificationRepository, never()).save(any());
    }

    @Test
    void approveMarksVerifiedAndAuditsDecision() {
        User owner = user(1L, "RECRUITER");
        Company company = company(10L, owner);
        CompanyVerification verification = new CompanyVerification();
        verification.setId(100L);
        verification.setCompany(company);
        verification.setStatus(VerificationStatus.PENDING);

        User admin = user(9L, "ADMIN");
        when(verificationRepository.findById(100L)).thenReturn(Optional.of(verification));
        when(verificationRepository.save(any(CompanyVerification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyVerificationResponse response = service.approve(100L, "Looks good", admin);

        assertEquals("VERIFIED", response.getStatus());
        verify(auditLogService).logAction(eq("CompanyVerification"), eq(100L), eq("APPROVE"), eq(admin), anyString(), any(), any());
        verify(notificationService).sendNotificationToUser(eq(owner), anyString(), anyString(), eq("COMPANY_VERIFICATION_DECISION"), anyString());
    }

    @Test
    void rejectRequiresANote() {
        User owner = user(1L, "RECRUITER");
        Company company = company(10L, owner);
        CompanyVerification verification = new CompanyVerification();
        verification.setId(100L);
        verification.setCompany(company);
        verification.setStatus(VerificationStatus.PENDING);

        when(verificationRepository.findById(100L)).thenReturn(Optional.of(verification));
        User admin = user(9L, "ADMIN");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.reject(100L, "  ", admin));
        assertEquals(400, ex.getStatusCode().value());
        verify(verificationRepository, never()).save(any());
    }

    private User user(Long id, String roleName) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("user" + id + "@example.com");
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    private Company company(Long id, User owner) {
        Company company = new Company();
        company.setId(id);
        company.setName("Acme Inc");
        company.setOwner(owner);
        return company;
    }
}
