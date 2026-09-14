package com.job_Portal_Backend.job_portal_backend.reports.service;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.Report;
import com.job_Portal_Backend.job_portal_backend.entity.Report.ReportStatus;
import com.job_Portal_Backend.job_portal_backend.entity.Report.ReportType;
import com.job_Portal_Backend.job_portal_backend.entity.Report.TargetType;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
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
import com.job_Portal_Backend.job_portal_backend.util.RoleUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ReportService {

    private static final Duration DUPLICATE_WINDOW = Duration.ofHours(24);
    private static final int MAX_REPORTS_PER_WINDOW = 10;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final ReportRepository reportRepository;
    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final RateLimitingService rateLimitingService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ReportService(ReportRepository reportRepository,
            JobRepository jobRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            MessageRepository messageRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            RateLimitingService rateLimitingService,
            AuditLogService auditLogService,
            NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.rateLimitingService = rateLimitingService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReportDto create(ReportRequest request, User reporter) {
        TargetType targetType = parseEnum(TargetType.class, request.getTargetType(), "target type");
        ReportType reportType = parseEnum(ReportType.class, request.getReportType(), "report type");

        validateReporterCanReportType(reporter, targetType);
        String targetSummary = validateTargetExistsAndSummarize(reporter, targetType, request.getTargetId());

        if (!rateLimitingService.isAllowed(reporter, "REPORT_SUBMIT", MAX_REPORTS_PER_WINDOW, RATE_LIMIT_WINDOW)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have submitted too many reports recently. Please try again later.");
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(
                reporter.getId(), targetType, request.getTargetId(), LocalDateTime.now().minus(DUPLICATE_WINDOW))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already reported this recently.");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(targetType);
        report.setTargetId(request.getTargetId());
        report.setReportType(reportType);
        report.setDescription(trimToNull(request.getDescription()));
        report.setStatus(ReportStatus.OPEN);
        Report saved = reportRepository.save(report);

        rateLimitingService.recordRequest(reporter, "REPORT_SUBMIT");
        auditLogService.logAction("Report", saved.getId(), "CREATE", reporter,
                "Reported " + targetType + " #" + request.getTargetId() + " for " + reportType, null, null);
        notifyAdmins(saved, targetSummary);

        return toDto(saved, targetSummary);
    }

    @Transactional(readOnly = true)
    public Page<ReportDto> adminList(String status, String targetTypeFilter, Pageable pageable) {
        ReportStatus statusEnum = isBlank(status) || "ALL".equalsIgnoreCase(status) ? null : parseEnum(ReportStatus.class, status, "status");
        TargetType targetTypeEnum = isBlank(targetTypeFilter) || "ALL".equalsIgnoreCase(targetTypeFilter)
                ? null : parseEnum(TargetType.class, targetTypeFilter, "target type");

        Page<Report> page;
        if (statusEnum != null && targetTypeEnum != null) {
            page = reportRepository.findByStatusAndTargetType(statusEnum, targetTypeEnum, pageable);
        } else if (statusEnum != null) {
            page = reportRepository.findByStatus(statusEnum, pageable);
        } else if (targetTypeEnum != null) {
            page = reportRepository.findByTargetType(targetTypeEnum, pageable);
        } else {
            page = reportRepository.findAll(pageable);
        }
        return page.map(report -> toDto(report, summarizeQuietly(report)));
    }

    @Transactional(readOnly = true)
    public ReportDto adminGet(Long reportId) {
        Report report = requireReport(reportId);
        return toDto(report, summarizeQuietly(report));
    }

    @Transactional
    public ReportDto markUnderReview(Long reportId, User admin) {
        Report report = requireReport(reportId);
        report.setStatus(ReportStatus.UNDER_REVIEW);
        Report saved = reportRepository.save(report);
        auditLogService.logAction("Report", saved.getId(), "REVIEW", admin, "Marked report under review", null, null);
        return toDto(saved, summarizeQuietly(saved));
    }

    @Transactional
    public ReportDto resolve(Long reportId, String note, boolean suspendTarget, User admin) {
        Report report = requireReport(reportId);
        report.setStatus(ReportStatus.RESOLVED);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolutionNote(trimToNull(note));
        Report saved = reportRepository.save(report);

        auditLogService.logAction("Report", saved.getId(), "RESOLVE", admin,
                "Resolved report" + (note != null ? ": " + note : ""), null, null);

        if (suspendTarget) {
            suspendTargetIfUser(saved, admin);
        }
        return toDto(saved, summarizeQuietly(saved));
    }

    @Transactional
    public ReportDto reject(Long reportId, String note, User admin) {
        Report report = requireReport(reportId);
        report.setStatus(ReportStatus.REJECTED);
        report.setResolvedBy(admin);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolutionNote(trimToNull(note));
        Report saved = reportRepository.save(report);

        auditLogService.logAction("Report", saved.getId(), "REJECT", admin,
                "Rejected report" + (note != null ? ": " + note : ""), null, null);
        return toDto(saved, summarizeQuietly(saved));
    }

    // --- helpers -------------------------------------------------------

    private void suspendTargetIfUser(Report report, User admin) {
        if (report.getTargetType() != TargetType.RECRUITER && report.getTargetType() != TargetType.CANDIDATE) {
            return;
        }
        userRepository.findById(report.getTargetId()).ifPresent(target -> {
            if (Boolean.TRUE.equals(target.getIsBlocked())) {
                return;
            }
            target.setIsBlocked(true);
            userRepository.save(target);
            auditLogService.logAction("User", target.getId(), "SUSPEND", admin,
                    "Suspended account following report #" + report.getId(), null, null);
        });
    }

    private void validateReporterCanReportType(User reporter, TargetType targetType) {
        String role = RoleUtils.resolvePrimaryRole(reporter.getRoles());
        Set<TargetType> allowed = "RECRUITER".equalsIgnoreCase(role)
                ? Set.of(TargetType.CANDIDATE, TargetType.MESSAGE)
                : Set.of(TargetType.JOB, TargetType.COMPANY, TargetType.RECRUITER, TargetType.MESSAGE);
        if (!allowed.contains(targetType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not permitted to report this type of content.");
        }
    }

    private String validateTargetExistsAndSummarize(User reporter, TargetType targetType, Long targetId) {
        return switch (targetType) {
            case JOB -> {
                Job job = jobRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
                yield "Job: " + job.getTitle();
            }
            case COMPANY -> {
                Company company = companyRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
                yield "Company: " + company.getName();
            }
            case RECRUITER -> {
                User target = requireUserWithRole(targetId, "RECRUITER");
                yield "Recruiter: " + fullName(target);
            }
            case CANDIDATE -> {
                User target = requireUserWithRole(targetId, "USER");
                yield "Candidate: " + fullName(target);
            }
            case MESSAGE -> {
                Message message = messageRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("Message not found"));
                boolean isParticipant = conversationParticipantRepository
                        .findByConversationIdAndUserId(message.getConversation().getId(), reporter.getId())
                        .isPresent();
                if (!isParticipant) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "You can only report messages from your own conversations.");
                }
                yield "Message from " + fullName(message.getSender()) + ": " + truncate(message.getContent());
            }
        };
    }

    private String summarizeQuietly(Report report) {
        try {
            return switch (report.getTargetType()) {
                case JOB -> jobRepository.findById(report.getTargetId()).map(Job::getTitle)
                        .map(title -> "Job: " + title).orElse("Job #" + report.getTargetId() + " (deleted)");
                case COMPANY -> companyRepository.findById(report.getTargetId()).map(Company::getName)
                        .map(name -> "Company: " + name).orElse("Company #" + report.getTargetId() + " (deleted)");
                case RECRUITER -> userRepository.findById(report.getTargetId()).map(this::fullName)
                        .map(name -> "Recruiter: " + name).orElse("Recruiter #" + report.getTargetId() + " (deleted)");
                case CANDIDATE -> userRepository.findById(report.getTargetId()).map(this::fullName)
                        .map(name -> "Candidate: " + name).orElse("Candidate #" + report.getTargetId() + " (deleted)");
                case MESSAGE -> messageRepository.findById(report.getTargetId())
                        .map(m -> "Message from " + fullName(m.getSender()) + ": " + truncate(m.getContent()))
                        .orElse("Message #" + report.getTargetId() + " (deleted)");
            };
        } catch (RuntimeException ex) {
            return report.getTargetType() + " #" + report.getTargetId();
        }
    }

    private User requireUserWithRole(Long userId, String expectedRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String role = RoleUtils.resolvePrimaryRole(user.getRoles());
        if (!expectedRole.equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target user does not match the reported type.");
        }
        return user;
    }

    private Report requireReport(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
    }

    private void notifyAdmins(Report report, String targetSummary) {
        List<User> admins = userRepository.findByRoleNameAndNotDeleted("ADMIN");
        if (!admins.isEmpty()) {
            notificationService.sendNotificationToUsers(admins,
                    "New report submitted",
                    "A " + report.getReportType() + " report was filed against " + targetSummary,
                    "REPORT_SUBMITTED");
        }
    }

    private ReportDto toDto(Report report, String targetSummary) {
        ReportDto dto = new ReportDto();
        dto.setId(report.getId());
        dto.setTargetType(report.getTargetType().name());
        dto.setTargetId(report.getTargetId());
        dto.setTargetSummary(targetSummary);
        dto.setReportType(report.getReportType().name());
        dto.setDescription(report.getDescription());
        dto.setStatus(report.getStatus().name());
        dto.setReporterName(fullName(report.getReporter()));
        dto.setReporterEmail(report.getReporter().getEmail());
        dto.setResolvedByName(report.getResolvedBy() != null ? fullName(report.getResolvedBy()) : null);
        dto.setResolvedAt(report.getResolvedAt());
        dto.setResolutionNote(report.getResolutionNote());
        dto.setCreatedAt(report.getCreatedAt());
        return dto;
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String label) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + label + ": " + value);
        }
    }

    private String fullName(User user) {
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() > 120 ? value.substring(0, 120) + "..." : value;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
