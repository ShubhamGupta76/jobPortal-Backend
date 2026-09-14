package com.job_Portal_Backend.job_portal_backend.reports.service;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.Report;
import com.job_Portal_Backend.job_portal_backend.entity.Report.ReportStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.Conversation;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.Message;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.ConversationParticipantRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageRepository;
import com.job_Portal_Backend.job_portal_backend.reports.dto.ReportDto;
import com.job_Portal_Backend.job_portal_backend.reports.dto.ReportRequest;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ReportRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import com.job_Portal_Backend.job_portal_backend.service.RateLimitingService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final CompanyRepository companyRepository = mock(CompanyRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final ConversationParticipantRepository conversationParticipantRepository = mock(ConversationParticipantRepository.class);
    private final RateLimitingService rateLimitingService = mock(RateLimitingService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NotificationService notificationService = mock(NotificationService.class);

    private final ReportService service = new ReportService(
            reportRepository, jobRepository, companyRepository, userRepository,
            messageRepository, conversationParticipantRepository, rateLimitingService,
            auditLogService, notificationService);

    @Test
    void candidateCanReportAJob() {
        User candidate = user(1L, "USER");
        Job job = new Job();
        job.setId(20L);
        job.setTitle("Backend Engineer");

        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(rateLimitingService.isAllowed(eq(candidate), eq("REPORT_SUBMIT"), anyInt(), any(Duration.class))).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(any(), any(), any(), any())).thenReturn(false);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(100L);
            return report;
        });
        when(userRepository.findByRoleNameAndNotDeleted("ADMIN")).thenReturn(List.of());

        ReportRequest request = new ReportRequest();
        request.setTargetType("JOB");
        request.setTargetId(20L);
        request.setReportType("MISLEADING_JOB");

        ReportDto dto = service.create(request, candidate);

        assertEquals("OPEN", dto.getStatus());
        assertTrue(dto.getTargetSummary().contains("Backend Engineer"));
        verify(rateLimitingService).recordRequest(candidate, "REPORT_SUBMIT");
    }

    @Test
    void recruiterCannotReportAJob() {
        User recruiter = user(2L, "RECRUITER");
        ReportRequest request = new ReportRequest();
        request.setTargetType("JOB");
        request.setTargetId(20L);
        request.setReportType("SPAM");

        assertThrows(ResponseStatusException.class, () -> service.create(request, recruiter));
        verifyNoInteractions(reportRepository);
    }

    @Test
    void duplicateReportWithinWindowIsRejected() {
        User candidate = user(1L, "USER");
        Job job = new Job();
        job.setId(20L);
        job.setTitle("Backend Engineer");

        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(rateLimitingService.isAllowed(eq(candidate), eq("REPORT_SUBMIT"), anyInt(), any(Duration.class))).thenReturn(true);
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(any(), any(), any(), any())).thenReturn(true);

        ReportRequest request = new ReportRequest();
        request.setTargetType("JOB");
        request.setTargetId(20L);
        request.setReportType("SPAM");

        assertThrows(ResponseStatusException.class, () -> service.create(request, candidate));
        verify(reportRepository, never()).save(any());
    }

    @Test
    void rateLimitExceededIsRejected() {
        User candidate = user(1L, "USER");
        Job job = new Job();
        job.setId(20L);
        job.setTitle("Backend Engineer");

        when(jobRepository.findById(20L)).thenReturn(Optional.of(job));
        when(rateLimitingService.isAllowed(eq(candidate), eq("REPORT_SUBMIT"), anyInt(), any(Duration.class))).thenReturn(false);

        ReportRequest request = new ReportRequest();
        request.setTargetType("JOB");
        request.setTargetId(20L);
        request.setReportType("SPAM");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(request, candidate));
        assertEquals(429, ex.getStatusCode().value());
        verify(reportRepository, never()).save(any());
    }

    @Test
    void cannotReportMessageFromAConversationYouAreNotIn() {
        User candidate = user(1L, "USER");
        Conversation conversation = new Conversation();
        conversation.setId(30L);
        Message message = new Message();
        message.setId(40L);
        message.setConversation(conversation);
        message.setSender(user(9L, "RECRUITER"));
        message.setContent("hello");

        when(messageRepository.findById(40L)).thenReturn(Optional.of(message));
        when(conversationParticipantRepository.findByConversationIdAndUserId(30L, 1L)).thenReturn(Optional.empty());

        ReportRequest request = new ReportRequest();
        request.setTargetType("MESSAGE");
        request.setTargetId(40L);
        request.setReportType("HARASSMENT");

        assertThrows(ResponseStatusException.class, () -> service.create(request, candidate));
    }

    @Test
    void resolvingWithSuspendTargetBlocksTheReportedUser() {
        User admin = user(99L, "ADMIN");
        User target = user(5L, "USER");
        target.setIsBlocked(false);

        Report report = new Report();
        report.setId(100L);
        report.setTargetType(Report.TargetType.CANDIDATE);
        report.setTargetId(5L);
        report.setReportType(Report.ReportType.HARASSMENT);
        report.setStatus(ReportStatus.OPEN);
        report.setReporter(user(1L, "RECRUITER"));

        when(reportRepository.findById(100L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(5L)).thenReturn(Optional.of(target));

        ReportDto dto = service.resolve(100L, "Confirmed harassment", true, admin);

        assertEquals("RESOLVED", dto.getStatus());
        assertTrue(target.getIsBlocked());
        verify(userRepository).save(target);
    }

    @Test
    void rejectSetsStatusWithoutSuspending() {
        User admin = user(99L, "ADMIN");
        Report report = new Report();
        report.setId(101L);
        report.setTargetType(Report.TargetType.JOB);
        report.setTargetId(20L);
        report.setReportType(Report.ReportType.SPAM);
        report.setStatus(ReportStatus.OPEN);
        report.setReporter(user(1L, "USER"));

        when(reportRepository.findById(101L)).thenReturn(Optional.of(report));
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobRepository.findById(20L)).thenReturn(Optional.empty());

        ReportDto dto = service.reject(101L, "Not enough evidence", admin);

        assertEquals("REJECTED", dto.getStatus());
        verify(userRepository, never()).save(any());
    }

    private User user(Long id, String roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
