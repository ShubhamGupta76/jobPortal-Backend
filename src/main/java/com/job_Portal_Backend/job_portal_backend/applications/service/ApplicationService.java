package com.job_Portal_Backend.job_portal_backend.applications.service;

import com.job_Portal_Backend.job_portal_backend.assessments.entity.Assessment;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.Result;
import com.job_Portal_Backend.job_portal_backend.assessments.entity.TestSession;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.AssessmentRepository;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.ResultRepository;
import com.job_Portal_Backend.job_portal_backend.assessments.repository.TestSessionRepository;
import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationCreateRequest;
import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationTimelineEventDto;
import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Application.ApplicationStatus;
import com.job_Portal_Backend.job_portal_backend.entity.ApplicationStatusHistory;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.ResumeDocument;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.mapper.ApplicationMapper;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationStatusHistoryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ResumeDocumentRepository;
import com.job_Portal_Backend.job_portal_backend.jobs.service.JobMatchingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final AssessmentRepository assessmentRepository;
    private final TestSessionRepository testSessionRepository;
    private final ResultRepository resultRepository;
    private final ApplicationMapper applicationMapper;
    private final NotificationService notificationService;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final JobMatchingService jobMatchingService;
    private final ResumeDocumentRepository resumeDocumentRepository;

    private final String uploadDir = "uploads/resumes/";

    public ApplicationService(
            ApplicationRepository applicationRepository,
            JobRepository jobRepository,
            AssessmentRepository assessmentRepository,
            TestSessionRepository testSessionRepository,
            ResultRepository resultRepository,
            ApplicationMapper applicationMapper,
            NotificationService notificationService,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            JobMatchingService jobMatchingService,
            ResumeDocumentRepository resumeDocumentRepository) {
        this.applicationRepository = applicationRepository;
        this.jobRepository = jobRepository;
        this.assessmentRepository = assessmentRepository;
        this.testSessionRepository = testSessionRepository;
        this.resultRepository = resultRepository;
        this.applicationMapper = applicationMapper;
        this.notificationService = notificationService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.jobMatchingService = jobMatchingService;
        this.resumeDocumentRepository = resumeDocumentRepository;
    }

    public ApplicationDto applyToJob(ApplicationCreateRequest request, User user) throws IOException {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Check if user already applied
        Optional<Application> existing = applicationRepository.findByUserIdAndJobId(user.getId(), job.getId());
        if (existing.isPresent()) {
            throw new RuntimeException("You have already applied to this job");
        }

        Application application = new Application();
        application.setUser(user);
        application.setJob(job);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setCoverLetter(request.getCoverLetter());
        application.setSource(normalizeSource(request.getSource()));

        if (request.getResumeDocumentId() != null) {
            ResumeDocument resume = resumeDocumentRepository
                    .findByIdAndUserId(request.getResumeDocumentId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Selected resume not found"));
            // Snapshot the resume's current file path onto the application. Renaming, replacing,
            // or deleting the resume later in the candidate's library must not change this value.
            application.setResumePath(resume.getFileUpload().getFilePath());
        } else if (request.getResume() != null && !request.getResume().isEmpty()) {
            String fileName = UUID.randomUUID().toString() + "_" + request.getResume().getOriginalFilename();
            Path filePath = Paths.get(uploadDir + fileName);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, request.getResume().getBytes());
            application.setResumePath(filePath.toString());
        }

        application = applicationRepository.save(application);
        recordStatusChange(application, null, Application.ApplicationStatus.APPLIED, user, "Application submitted");
        notificationService.sendNotificationToUser(
                job.getRecruiter().getId(),
                "APPLICATION",
                "New application received",
                user.getFirstName() + " applied for " + job.getTitle() + ".",
                application.getId().toString());
        notificationService.sendNotificationToUser(
                user.getId(),
                "APPLICATION",
                "Application submitted",
                "Your application for " + job.getTitle() + " has been submitted.",
                application.getId().toString());
        return toDto(application);
    }

    public ApplicationDto updateApplicationStatus(Long applicationId, String status, User recruiter) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getJob().getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to update this application");
        }

        ApplicationStatus nextStatus = parseStatus(status);
        validateStatusTransition(application, nextStatus);

        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(nextStatus);
        application = applicationRepository.save(application);
        recordStatusChange(application, previousStatus, nextStatus, recruiter, "Status updated by recruiter");
        notificationService.createNotification(
                application.getUser(),
                "Application status updated",
                "Your application for " + application.getJob().getTitle() + " is now " + nextStatus + ".",
                "APPLICATION_STATUS",
                application.getId(),
                "APPLICATION");
        return toDto(application);
    }

    public ApplicationDto assignAssessment(Long jobId, Long candidateId, Long assessmentId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to assign assessments for this job");
        }

        Application application = applicationRepository.findByUserIdAndJobIdAndNotDeleted(candidateId, jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found for this candidate and job"));

        if (application.getStatus() != ApplicationStatus.SHORTLISTED &&
                application.getStatus() != ApplicationStatus.ASSESSMENT) {
            throw new RuntimeException("Candidate must be shortlisted before assessment assignment");
        }

        Assessment assessment = resolveAssessment(jobId, assessmentId, recruiter.getId());

        application.setAssignedAssessment(assessment);
        ApplicationStatus previousStatus = application.getStatus();
        application.setStatus(ApplicationStatus.ASSESSMENT);
        application = applicationRepository.save(application);
        if (previousStatus != ApplicationStatus.ASSESSMENT) {
            recordStatusChange(application, previousStatus, ApplicationStatus.ASSESSMENT, recruiter, "Assessment assigned");
        }

        notificationService.createNotification(
                application.getUser(),
                "Assessment assigned",
                "An assessment has been assigned for " + application.getJob().getTitle() + ".",
                "ASSESSMENT",
                application.getId(),
                "APPLICATION");
        return toDto(application);
    }

    public List<ApplicationDto> getApplicationsByUser(User user) {
        List<Application> applications = applicationRepository.findByUserIdAndNotDeleted(user.getId());
        return applications.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ApplicationTimelineEventDto> getApplicationTimeline(Long applicationId, User viewer) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        boolean candidateOwner = application.getUser().getId().equals(viewer.getId());
        boolean recruiterOwner = application.getJob().getRecruiter().getId().equals(viewer.getId());
        if (!candidateOwner && !recruiterOwner) {
            throw new RuntimeException("Unauthorized to view this application timeline");
        }
        return statusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(this::toTimelineEvent)
                .toList();
    }

    public Page<ApplicationDto> getApplicationsByRecruiter(User recruiter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Application> applications = applicationRepository.findByJobRecruiterIdAndNotDeleted(recruiter.getId(), pageable);
        return applications.map(this::toDto);
    }

    public List<ApplicationDto> getApplicationsByJob(Long jobId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to view applications for this job");
        }

        List<Application> applications = applicationRepository.findByJobIdAndNotDeleted(jobId);

        // Batch-fetch every application's timeline in one query instead of one query per
        // application (this list can be large for a popular job).
        List<Long> applicationIds = applications.stream().map(Application::getId).toList();
        Map<Long, List<ApplicationStatusHistory>> timelinesByApplicationId = applicationIds.isEmpty()
                ? Collections.emptyMap()
                : statusHistoryRepository.findByApplicationIdInOrderByCreatedAtAsc(applicationIds).stream()
                        .collect(Collectors.groupingBy(history -> history.getApplication().getId()));

        return applications.stream()
                .sorted((first, second) -> second.getCreatedAt().compareTo(first.getCreatedAt()))
                .map(application -> toDto(application, timelinesByApplicationId))
                .collect(Collectors.toList());
    }

    private ApplicationDto toDto(Application application) {
        return toDto(application, null);
    }

    /**
     * @param timelinesByApplicationId when supplied (list endpoints), avoids one
     *                                 findByApplicationIdOrderByCreatedAtAsc query per
     *                                 application; when null (single-application call sites),
     *                                 falls back to the per-application query, unchanged.
     */
    private ApplicationDto toDto(Application application, Map<Long, List<ApplicationStatusHistory>> timelinesByApplicationId) {
        ApplicationDto dto = applicationMapper.toDto(application);
        JobMatchingService.MatchResult match = jobMatchingService.score(application.getJob(), application.getUser());
        dto.setMatchScore(match.score());
        dto.setMatchingSkills(match.matchingSkills());
        dto.setMissingSkills(match.missingSkills());
        dto.setSkillsScore(match.skillsScore());
        dto.setExperienceScore(match.experienceScore());
        dto.setLocationScore(match.locationScore());
        dto.setPreferenceScore(match.preferenceScore());
        dto.setProfileScore(match.profileScore());
        List<ApplicationStatusHistory> timeline = timelinesByApplicationId != null
                ? timelinesByApplicationId.getOrDefault(application.getId(), Collections.emptyList())
                : statusHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(application.getId());
        dto.setTimeline(timeline.stream()
            .map(this::toTimelineEvent)
            .toList());

        if (application.getAssignedAssessment() == null) {
            return dto;
        }

        List<TestSession> sessions = testSessionRepository.findByAssessmentIdAndCandidateId(
                application.getAssignedAssessment().getId(),
                application.getUser().getId());

        sessions.stream()
                .max((first, second) -> first.getUpdatedAt().compareTo(second.getUpdatedAt()))
                .ifPresent(session -> {
                    dto.setAssessmentSessionId(session.getId());
                    Optional<Result> result = resultRepository.findByTestSessionId(session.getId());
                    result.ifPresent(value -> {
                        dto.setAssessmentScore(value.getPercentageScore());
                        dto.setAssessmentPassed(value.getPassed());
                        dto.setCorrectAnswers(value.getCorrectAnswers());
                        dto.setTotalQuestions(value.getTotalQuestions());
                        dto.setAssessmentAnalysis(value.getDetailedAnalysis());
                    });
                });

        return dto;
    }

    private void recordStatusChange(Application application, ApplicationStatus previousStatus,
                                    ApplicationStatus nextStatus, User actor, String note) {
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setPreviousStatus(previousStatus);
        history.setStatus(nextStatus);
        history.setActor(actor);
        history.setNote(note);
        statusHistoryRepository.save(history);
    }

    private ApplicationTimelineEventDto toTimelineEvent(ApplicationStatusHistory history) {
        ApplicationTimelineEventDto event = new ApplicationTimelineEventDto();
        event.setId(history.getId());
        event.setPreviousStatus(history.getPreviousStatus() == null ? null : history.getPreviousStatus().name());
        event.setStatus(history.getStatus().name());
        event.setActorName((history.getActor().getFirstName() + " " + history.getActor().getLastName()).trim());
        event.setTimestamp(history.getCreatedAt());
        event.setNote(history.getNote());
        return event;
    }

    private ApplicationStatus parseStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new RuntimeException("Invalid application status: " + status);
        }
    }

    private void validateStatusTransition(Application application, ApplicationStatus nextStatus) {
        ApplicationStatus currentStatus = application.getStatus();

        if (currentStatus == nextStatus) {
            return;
        }

        if (currentStatus == ApplicationStatus.HIRED || currentStatus == ApplicationStatus.REJECTED) {
            throw new RuntimeException("Cannot update an application that is already closed");
        }

        if (nextStatus == ApplicationStatus.REJECTED) {
            return;
        }

        if (currentStatus == ApplicationStatus.APPLIED && nextStatus == ApplicationStatus.SHORTLISTED) {
            return;
        }

        if (currentStatus == ApplicationStatus.SHORTLISTED && nextStatus == ApplicationStatus.ASSESSMENT) {
            if (application.getAssignedAssessment() == null) {
                throw new RuntimeException("Assign an assessment before moving to ASSESSMENT");
            }
            return;
        }

        if (currentStatus == ApplicationStatus.ASSESSMENT && nextStatus == ApplicationStatus.INTERVIEW) {
            return;
        }

        if (currentStatus == ApplicationStatus.INTERVIEW && nextStatus == ApplicationStatus.HIRED) {
            return;
        }

        throw new RuntimeException("Invalid status transition: " + currentStatus + " -> " + nextStatus);
    }

    private Assessment resolveAssessment(Long jobId, Long assessmentId, Long recruiterId) {
        if (assessmentId != null) {
            Assessment assessment = assessmentRepository.findById(assessmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

            if (!assessment.getRecruiter().getId().equals(recruiterId)) {
                throw new RuntimeException("Unauthorized to assign this assessment");
            }

            if (assessment.getStatus() != Assessment.AssessmentStatus.PUBLISHED
                    && assessment.getStatus() != Assessment.AssessmentStatus.LIVE) {
                throw new RuntimeException("Only published assessments can be assigned to candidates");
            }

            return assessment;
        }

        return assessmentRepository.findAssignableAssessmentsByJobId(jobId).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No published assessment found for this job"));
    }

    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return "DIRECT";
        }

        return source.trim().replace(' ', '_').toUpperCase();
    }
}
