package com.job_Portal_Backend.job_portal_backend.companyverification.service;

import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationDocumentDto;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationHistoryDto;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationRequest;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationResponse;
import com.job_Portal_Backend.job_portal_backend.companyverification.dto.CompanyVerificationSummaryDto;
import com.job_Portal_Backend.job_portal_backend.dto.FileUploadDto;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification.VerificationStatus;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerificationDocument;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerificationHistory;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationDocumentRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationHistoryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.AuditLogService;
import com.job_Portal_Backend.job_portal_backend.service.FileUploadService;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class CompanyVerificationService {

    private static final Set<VerificationStatus> RESUBMITTABLE_STATUSES = Set.of(
            VerificationStatus.REJECTED, VerificationStatus.UNDER_REVIEW, VerificationStatus.EXPIRED);

    private final CompanyVerificationRepository verificationRepository;
    private final CompanyVerificationDocumentRepository documentRepository;
    private final CompanyVerificationHistoryRepository historyRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public CompanyVerificationService(CompanyVerificationRepository verificationRepository,
            CompanyVerificationDocumentRepository documentRepository,
            CompanyVerificationHistoryRepository historyRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            FileUploadService fileUploadService,
            AuditLogService auditLogService,
            NotificationService notificationService) {
        this.verificationRepository = verificationRepository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.fileUploadService = fileUploadService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CompanyVerificationResponse submit(Long companyId, CompanyVerificationRequest request, User actor) {
        Company company = ownedCompany(companyId, actor);

        CompanyVerification verification = verificationRepository.findByCompanyId(companyId).orElse(null);
        if (verification != null && !RESUBMITTABLE_STATUSES.contains(verification.getStatus())) {
            String reason = verification.getStatus() == VerificationStatus.VERIFIED
                    ? "This company is already verified."
                    : "A verification request is already awaiting review.";
            throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
        }

        if (verification == null) {
            verification = new CompanyVerification();
            verification.setCompany(company);
        }

        verification.setStatus(VerificationStatus.PENDING);
        verification.setLegalName(request.getLegalName().trim());
        verification.setRegistrationNumber(trimToNull(request.getRegistrationNumber()));
        verification.setCompanyType(trimToNull(request.getCompanyType()));
        verification.setOfficialWebsite(trimToNull(request.getOfficialWebsite()));
        verification.setOfficialEmailDomain(trimToNull(request.getOfficialEmailDomain()));
        verification.setSubmittedBy(actor);
        verification.setSubmittedAt(LocalDateTime.now());
        verification.setReviewedBy(null);
        verification.setReviewedAt(null);
        verification.setLatestNote(null);

        CompanyVerification saved = verificationRepository.save(verification);

        documentRepository.deleteByVerificationId(saved.getId());
        attachDocuments(saved, request.getDocumentFileIds(), actor);

        recordHistory(saved, VerificationStatus.PENDING, actor, "Verification submitted for review.");

        notifyAdmins(saved, "New company verification request",
                company.getName() + " submitted a verification request.");

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CompanyVerificationResponse getForCompany(Long companyId, User viewer) {
        Company company = viewer.getRoles().stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()))
                ? companyRepository.findById(companyId)
                        .orElseThrow(() -> new ResourceNotFoundException("Company not found"))
                : ownedCompany(companyId, viewer);

        return verificationRepository.findByCompanyId(company.getId())
                .map(this::toResponse)
                .orElse(unverifiedResponse(company));
    }

    @Transactional(readOnly = true)
    public Page<CompanyVerificationSummaryDto> getQueue(String statusFilter, Pageable pageable) {
        List<VerificationStatus> statuses = statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter)
                ? List.of(VerificationStatus.values())
                : List.of(VerificationStatus.valueOf(statusFilter.trim().toUpperCase()));
        return verificationRepository.findByStatusIn(statuses, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public CompanyVerificationResponse getDetail(Long verificationId) {
        return toResponse(requireVerification(verificationId));
    }

    @Transactional
    public CompanyVerificationResponse approve(Long verificationId, String note, User admin) {
        CompanyVerification verification = requireVerification(verificationId);
        verification.setStatus(VerificationStatus.VERIFIED);
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setLatestNote(note);
        CompanyVerification saved = verificationRepository.save(verification);

        recordHistory(saved, VerificationStatus.VERIFIED, admin, note);
        auditLogService.logAction("CompanyVerification", saved.getId(), "APPROVE", admin,
                "Approved company verification for " + saved.getCompany().getName(), null, null);
        notifyOwner(saved, "Your company is now verified",
                saved.getCompany().getName() + " has been verified. A verified badge is now shown on your company profile.");
        return toResponse(saved);
    }

    @Transactional
    public CompanyVerificationResponse reject(Long verificationId, String note, User admin) {
        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required.");
        }
        CompanyVerification verification = requireVerification(verificationId);
        verification.setStatus(VerificationStatus.REJECTED);
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setLatestNote(note);
        CompanyVerification saved = verificationRepository.save(verification);

        recordHistory(saved, VerificationStatus.REJECTED, admin, note);
        auditLogService.logAction("CompanyVerification", saved.getId(), "REJECT", admin,
                "Rejected company verification for " + saved.getCompany().getName() + ": " + note, null, null);
        notifyOwner(saved, "Company verification rejected",
                "Your verification request was rejected: " + note);
        return toResponse(saved);
    }

    @Transactional
    public CompanyVerificationResponse requestMoreInfo(Long verificationId, String note, User admin) {
        if (note == null || note.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please describe what additional information is needed.");
        }
        CompanyVerification verification = requireVerification(verificationId);
        verification.setStatus(VerificationStatus.UNDER_REVIEW);
        verification.setReviewedBy(admin);
        verification.setReviewedAt(LocalDateTime.now());
        verification.setLatestNote(note);
        CompanyVerification saved = verificationRepository.save(verification);

        recordHistory(saved, VerificationStatus.UNDER_REVIEW, admin, note);
        auditLogService.logAction("CompanyVerification", saved.getId(), "REQUEST_INFO", admin,
                "Requested more information for " + saved.getCompany().getName(), null, null);
        notifyOwner(saved, "More information needed for verification",
                "The reviewer requested more information: " + note);
        return toResponse(saved);
    }


    private Company ownedCompany(Long companyId, User actor) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
        if (company.getOwner() == null || !company.getOwner().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this company profile.");
        }
        return company;
    }

    private CompanyVerification requireVerification(Long verificationId) {
        return verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification request not found"));
    }

    private void attachDocuments(CompanyVerification verification, List<Long> fileIds, User actor) {
        if (fileIds == null) {
            return;
        }
        for (Long fileId : fileIds) {
            if (fileId == null) {
                continue;
            }
            FileUploadDto file = fileUploadService.getFileById(fileId, actor);
            if (file == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "One of the uploaded documents could not be verified as yours.");
            }
            CompanyVerificationDocument document = new CompanyVerificationDocument();
            document.setVerification(verification);
            document.setFileUpload(toFileUploadReference(fileId));
            document.setLabel(file.getOriginalFilename());
            documentRepository.save(document);
        }
    }

    private com.job_Portal_Backend.job_portal_backend.entity.FileUpload toFileUploadReference(Long fileId) {
        com.job_Portal_Backend.job_portal_backend.entity.FileUpload reference = new com.job_Portal_Backend.job_portal_backend.entity.FileUpload();
        reference.setId(fileId);
        return reference;
    }

    private void recordHistory(CompanyVerification verification, VerificationStatus status, User actor, String note) {
        CompanyVerificationHistory history = new CompanyVerificationHistory();
        history.setVerification(verification);
        history.setStatus(status);
        history.setActor(actor);
        history.setNote(note);
        historyRepository.save(history);
    }

    private void notifyAdmins(CompanyVerification verification, String title, String message) {
        List<User> admins = userRepository.findByRoleNameAndNotDeleted("ADMIN");
        if (!admins.isEmpty()) {
            notificationService.sendNotificationToUsers(admins, title, message, "COMPANY_VERIFICATION_SUBMITTED");
        }
    }

    private void notifyOwner(CompanyVerification verification, String title, String message) {
        User owner = verification.getCompany().getOwner();
        if (owner != null) {
            notificationService.sendNotificationToUser(owner, title, message, "COMPANY_VERIFICATION_DECISION",
                    "{\"companyId\":" + verification.getCompany().getId() + ",\"status\":\"" + verification.getStatus() + "\"}");
        }
    }

    private CompanyVerificationResponse unverifiedResponse(Company company) {
        CompanyVerificationResponse response = new CompanyVerificationResponse();
        response.setCompanyId(company.getId());
        response.setCompanyName(company.getName());
        response.setStatus("UNVERIFIED");
        response.setDocuments(List.of());
        response.setHistory(List.of());
        return response;
    }

    private CompanyVerificationSummaryDto toSummary(CompanyVerification verification) {
        CompanyVerificationSummaryDto dto = new CompanyVerificationSummaryDto();
        dto.setId(verification.getId());
        dto.setCompanyId(verification.getCompany().getId());
        dto.setCompanyName(verification.getCompany().getName());
        dto.setStatus(verification.getStatus().name());
        dto.setSubmittedByName(fullName(verification.getSubmittedBy()));
        dto.setSubmittedAt(verification.getSubmittedAt());
        dto.setReviewedAt(verification.getReviewedAt());
        return dto;
    }

    private CompanyVerificationResponse toResponse(CompanyVerification verification) {
        CompanyVerificationResponse response = new CompanyVerificationResponse();
        response.setId(verification.getId());
        response.setCompanyId(verification.getCompany().getId());
        response.setCompanyName(verification.getCompany().getName());
        response.setStatus(verification.getStatus().name());
        response.setLegalName(verification.getLegalName());
        response.setRegistrationNumber(verification.getRegistrationNumber());
        response.setCompanyType(verification.getCompanyType());
        response.setOfficialWebsite(verification.getOfficialWebsite());
        response.setOfficialEmailDomain(verification.getOfficialEmailDomain());
        response.setSubmittedByName(fullName(verification.getSubmittedBy()));
        response.setSubmittedAt(verification.getSubmittedAt());
        response.setReviewedByName(fullName(verification.getReviewedBy()));
        response.setReviewedAt(verification.getReviewedAt());
        response.setLatestNote(verification.getLatestNote());

        List<CompanyVerificationDocumentDto> documents = documentRepository
                .findByVerificationIdOrderByAttachedAtDesc(verification.getId())
                .stream()
                .map(this::toDocumentDto)
                .toList();
        response.setDocuments(documents);

        List<CompanyVerificationHistoryDto> history = historyRepository
                .findByVerificationIdOrderByCreatedAtAsc(verification.getId())
                .stream()
                .map(this::toHistoryDto)
                .toList();
        response.setHistory(history);

        return response;
    }

    private CompanyVerificationDocumentDto toDocumentDto(CompanyVerificationDocument document) {
        CompanyVerificationDocumentDto dto = new CompanyVerificationDocumentDto();
        dto.setId(document.getId());
        Long fileId = document.getFileUpload().getId();
        dto.setFileId(fileId);
        FileUploadDto file = fileUploadService.getFileByIdForAdmin(fileId);
        if (file != null) {
            dto.setFilename(file.getOriginalFilename());
            dto.setContentType(file.getContentType());
            dto.setFileSize(file.getFileSize());
        }
        dto.setDownloadUrl("/api/v1/admin/company-verifications/documents/" + fileId + "/download");
        dto.setLabel(document.getLabel());
        dto.setAttachedAt(document.getAttachedAt());
        return dto;
    }

    private CompanyVerificationHistoryDto toHistoryDto(CompanyVerificationHistory history) {
        CompanyVerificationHistoryDto dto = new CompanyVerificationHistoryDto();
        dto.setStatus(history.getStatus().name());
        dto.setActorName(fullName(history.getActor()));
        dto.setNote(history.getNote());
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }

    private String fullName(User user) {
        if (user == null) {
            return null;
        }
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
