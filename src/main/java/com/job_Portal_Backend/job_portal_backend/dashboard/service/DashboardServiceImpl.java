package com.job_Portal_Backend.job_portal_backend.dashboard.service;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.DashboardActivityItemDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.CandidateDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.ProfileStrengthDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.PublicMetricsResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.RecruiterJobPerformanceDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.RecruiterDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewDto;
import com.job_Portal_Backend.job_portal_backend.dto.NotificationDto;
import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Interview;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.mapper.ApplicationMapper;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.InterviewRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DashboardServiceImpl implements DashboardService {

        private final ApplicationRepository applicationRepository;
        private final BookmarkRepository bookmarkRepository;
        private final JobRepository jobRepository;
        private final CompanyRepository companyRepository;
        private final UserRepository userRepository;
        private final InterviewRepository interviewRepository;
        private final NotificationService notificationService;
        private final JobMapper jobMapper;
        private final ApplicationMapper applicationMapper;

        public DashboardServiceImpl(
                        ApplicationRepository applicationRepository,
                        BookmarkRepository bookmarkRepository,
                        JobRepository jobRepository,
                        CompanyRepository companyRepository,
                        UserRepository userRepository,
                        InterviewRepository interviewRepository,
                        NotificationService notificationService,
                        JobMapper jobMapper,
                        ApplicationMapper applicationMapper) {
                this.applicationRepository = applicationRepository;
                this.bookmarkRepository = bookmarkRepository;
                this.jobRepository = jobRepository;
                this.companyRepository = companyRepository;
                this.userRepository = userRepository;
                this.interviewRepository = interviewRepository;
                this.notificationService = notificationService;
                this.jobMapper = jobMapper;
                this.applicationMapper = applicationMapper;
        }

        @Override
        public CandidateDashboardResponse getCandidateDashboard(User user) {
                CandidateDashboardResponse response = new CandidateDashboardResponse();
                response.setFirstName(user.getFirstName());
                response.setFullName(buildFullName(user));
                long appliedJobs = applicationRepository.countByUserIdAndNotDeleted(user.getId());
                long activeApplications = appliedJobs
                                - applicationRepository.countByUserIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "HIRED")
                                - applicationRepository.countByUserIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "REJECTED");
                response.setAppliedJobs(appliedJobs);
                response.setActiveApplications(Math.max(0, activeApplications));
                response.setSavedJobs(bookmarkRepository.countByUserId(user.getId()));
                response.setUnreadNotifications(notificationService.getUnreadNotificationCount(user.getId()));
                response.setPendingAssessments(applicationRepository.countByUserIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "ASSESSMENT"));
                response.setResumeAvailable(hasText(user.getResumePath()));

                List<ApplicationDto> recentApplications = applicationRepository.findByUserIdAndNotDeleted(user.getId())
                                .stream()
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .limit(5)
                                .map(applicationMapper::toDto)
                                .toList();
                response.setRecentApplications(recentApplications);

                List<JobDto> savedJobListings = bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                                .stream()
                                .limit(5)
                                .map(bookmark -> jobMapper.toDto(bookmark.getJob()))
                                .toList();
                response.setSavedJobListings(savedJobListings);

                List<Interview> upcomingInterviews = interviewRepository.findUpcomingInterviewsByCandidate(user,
                                LocalDateTime.now());
                Set<Long> scheduledInterviewJobIds = upcomingInterviews.stream()
                                .map(interview -> interview.getJob().getId())
                                .collect(java.util.stream.Collectors.toSet());
                List<Application> interviewStageApplications = applicationRepository.findByUserIdAndNotDeleted(user.getId())
                                .stream()
                                .filter(application -> application.getStatus() == Application.ApplicationStatus.INTERVIEW)
                                .filter(application -> !scheduledInterviewJobIds.contains(application.getJob().getId()))
                                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                                .toList();

                response.setUpcomingInterviews(upcomingInterviews.size() + interviewStageApplications.size());
                response.setNextInterviewAt(upcomingInterviews.isEmpty() ? null : upcomingInterviews.get(0).getScheduledAt());
                List<InterviewDto> interviewSchedule = new ArrayList<>();
                upcomingInterviews.stream()
                                .limit(4)
                                .map(this::mapInterviewToDto)
                                .forEach(interviewSchedule::add);
                interviewStageApplications.stream()
                                .limit(Math.max(0, 4 - interviewSchedule.size()))
                                .map(this::mapPendingInterviewApplicationToDto)
                                .forEach(interviewSchedule::add);
                response.setUpcomingInterviewSchedule(interviewSchedule);

                response.setProfileStrength(buildProfileStrength(user));
                response.setRecommendedJobs(buildRecommendedJobs(user));
                response.setRecentActivities(buildRecentActivities(user, recentApplications, upcomingInterviews));

                return response;
        }

        @Override
        public RecruiterDashboardResponse getRecruiterDashboard(User user) {
                RecruiterDashboardResponse response = new RecruiterDashboardResponse();
                response.setFirstName(user.getFirstName());
                response.setFullName(buildFullName(user));
                long totalJobs = jobRepository.countByRecruiterIdAndNotDeleted(user.getId());
                long totalApplicants = applicationRepository.countByJobRecruiterIdAndNotDeleted(user.getId());
                long shortlistedCount = applicationRepository
                                .countByJobRecruiterIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "SHORTLISTED");
                long interviewCount = applicationRepository
                                .countByJobRecruiterIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "INTERVIEW");
                long hiredCount = applicationRepository
                                .countByJobRecruiterIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "HIRED");
                long pendingReviews = applicationRepository
                                .countByJobRecruiterIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "APPLIED");

                response.setTotalJobs(totalJobs);
                response.setOpenPositions(totalJobs);
                response.setTotalApplicants(totalApplicants);
                response.setShortlistedCount(shortlistedCount);
                response.setInterviewCount(interviewCount);
                response.setHiredCount(hiredCount);
                response.setPendingReviews(pendingReviews);
                response.setOfferAcceptRate(totalApplicants == 0 ? 0 : (hiredCount * 100.0) / totalApplicants);

                List<JobDto> recentJobs = jobRepository.findByRecruiterIdAndNotDeleted(user.getId())
                                .stream()
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .limit(5)
                                .map(jobMapper::toDto)
                                .toList();
                response.setRecentJobs(recentJobs);

                List<ApplicationDto> recentApplicants = applicationRepository
                                .findByJobRecruiterIdAndNotDeleted(user.getId(),
                                                PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("createdAt").descending()))
                                .stream()
                                .map(applicationMapper::toDto)
                                .toList();
                response.setRecentApplicants(recentApplicants);

                List<Application> recruiterApplications = applicationRepository.findAllByJobRecruiterIdAndNotDeleted(user.getId());
                response.setAverageTimeToHireDays(calculateAverageTimeToHire(recruiterApplications));
                response.setActiveJobPerformance(buildRecruiterJobPerformance(user));

                return response;
        }

        @Override
        public PublicMetricsResponse getPublicMetrics() {
                return new PublicMetricsResponse(
                                jobRepository.count(),
                                applicationRepository.count(),
                                companyRepository.count(),
                                userRepository.count());
        }

        private List<JobDto> buildRecommendedJobs(User user) {
                Set<Long> excludedJobIds = new HashSet<>();
                applicationRepository.findByUserIdAndNotDeleted(user.getId())
                                .forEach(application -> excludedJobIds.add(application.getJob().getId()));
                bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                                .forEach(bookmark -> excludedJobIds.add(bookmark.getJob().getId()));

                return jobRepository.findJobsWithFilters(null, null, null, null, null, null)
                                .stream()
                                .filter(job -> !excludedJobIds.contains(job.getId()))
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .limit(4)
                                .map(jobMapper::toDto)
                                .toList();
        }

        private ProfileStrengthDto buildProfileStrength(User user) {
                int score = 0;
                if (hasText(user.getFirstName())) score += 15;
                if (hasText(user.getLastName())) score += 10;
                if (hasText(user.getPhone())) score += 15;
                if (hasText(user.getHeadline())) score += 15;
                if (hasText(user.getBio())) score += 20;
                if (hasText(user.getLocation())) score += 10;
                if (hasText(user.getResumePath())) score += 15;

                String level;
                if (score >= 80) {
                        level = "Advanced";
                } else if (score >= 50) {
                        level = "Intermediate";
                } else {
                        level = "Starter";
                }

                String message;
                if (score >= 90) {
                        message = "Your profile looks strong and recruiter-ready.";
                } else if (score >= 60) {
                        message = "Almost there. Complete a few more details to strengthen applications.";
                } else {
                        message = "Add more profile details to improve recommendations and recruiter visibility.";
                }

                return ProfileStrengthDto.builder()
                                .completionPercentage(score)
                                .level(level)
                                .message(message)
                                .build();
        }

        private List<DashboardActivityItemDto> buildRecentActivities(
                        User user,
                        List<ApplicationDto> recentApplications,
                        List<Interview> upcomingInterviews) {
                List<DashboardActivityItemDto> activityItems = new ArrayList<>();

                recentApplications.stream()
                                .limit(3)
                                .forEach(application -> activityItems.add(DashboardActivityItemDto.builder()
                                                .type("APPLICATION")
                                                .title("Applied to " + application.getJobTitle())
                                                .subtitle("Application status: " + application.getStatus())
                                                .timestamp(application.getCreatedAt())
                                                .build()));

                upcomingInterviews.stream()
                                .limit(3)
                                .forEach(interview -> activityItems.add(DashboardActivityItemDto.builder()
                                                .type("INTERVIEW")
                                                .title("Interview scheduled for " + interview.getJob().getTitle())
                                                .subtitle("Starts at " + interview.getScheduledAt())
                                                .timestamp(interview.getCreatedAt())
                                                .build()));

                List<NotificationDto> notifications = notificationService.getUserNotifications(user, false);
                notifications.stream()
                                .limit(3)
                                .forEach(notification -> activityItems.add(DashboardActivityItemDto.builder()
                                                .type(notification.getType())
                                                .title(notification.getTitle())
                                                .subtitle(notification.getMessage())
                                                .timestamp(notification.getCreatedAt())
                                                .build()));

                return activityItems.stream()
                                .filter(item -> item.getTimestamp() != null)
                                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                                .limit(6)
                                .toList();
        }

        private InterviewDto mapInterviewToDto(Interview interview) {
                return InterviewDto.builder()
                                .id(interview.getId())
                                .jobId(interview.getJob().getId())
                                .jobTitle(interview.getJob().getTitle())
                                .candidateId(interview.getCandidate().getId())
                                .candidateName(buildFullName(interview.getCandidate()))
                                .candidateEmail(interview.getCandidate().getEmail())
                                .recruiterId(interview.getRecruiter().getId())
                                .recruiterName(buildFullName(interview.getRecruiter()))
                                .recruiterEmail(interview.getRecruiter().getEmail())
                                .scheduledAt(interview.getScheduledAt())
                                .type(interview.getType().name())
                                .status(interview.getStatus().name())
                                .notes(interview.getNotes())
                                .meetingLink(interview.getMeetingLink())
                                .location(interview.getLocation())
                                .durationMinutes(interview.getDurationMinutes())
                                .createdAt(interview.getCreatedAt())
                                .updatedAt(interview.getUpdatedAt())
                                .build();
        }

        private InterviewDto mapPendingInterviewApplicationToDto(Application application) {
                return InterviewDto.builder()
                                .id(application.getId())
                                .jobId(application.getJob().getId())
                                .jobTitle(application.getJob().getTitle())
                                .candidateId(application.getUser().getId())
                                .candidateName(buildFullName(application.getUser()))
                                .candidateEmail(application.getUser().getEmail())
                                .recruiterId(application.getJob().getRecruiter().getId())
                                .recruiterName(buildFullName(application.getJob().getRecruiter()))
                                .recruiterEmail(application.getJob().getRecruiter().getEmail())
                                .scheduledAt(null)
                                .type("INTERVIEW")
                                .status("PENDING_SCHEDULE")
                                .notes("Recruiter moved your application to interview stage.")
                                .durationMinutes(null)
                                .createdAt(application.getCreatedAt())
                                .updatedAt(application.getUpdatedAt())
                                .build();
        }

        private String buildFullName(User user) {
                return ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        }

        private boolean hasText(String value) {
                return value != null && !value.isBlank();
        }

        private long calculateAverageTimeToHire(List<Application> applications) {
                List<Application> hiredApplications = applications.stream()
                                .filter(application -> application.getStatus() == Application.ApplicationStatus.HIRED)
                                .toList();
                if (hiredApplications.isEmpty()) {
                        return 0;
                }

                long totalDays = hiredApplications.stream()
                                .mapToLong(application -> Math.max(0,
                                                ChronoUnit.DAYS.between(application.getCreatedAt(), application.getUpdatedAt())))
                                .sum();
                return Math.round((double) totalDays / hiredApplications.size());
        }

        private List<RecruiterJobPerformanceDto> buildRecruiterJobPerformance(User user) {
                return jobRepository.findByRecruiterIdAndNotDeleted(user.getId())
                                .stream()
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .limit(5)
                                .map(job -> RecruiterJobPerformanceDto.builder()
                                                .jobId(job.getId())
                                                .title(job.getTitle())
                                                .category(hasText(job.getJobType()) ? job.getJobType() : job.getExperienceLevel())
                                                .applicants(applicationRepository.countByJobIdAndNotDeleted(job.getId()))
                                                .status(hasText(job.getStatus()) ? job.getStatus() : "ACTIVE")
                                                .build())
                                .toList();
        }
}
