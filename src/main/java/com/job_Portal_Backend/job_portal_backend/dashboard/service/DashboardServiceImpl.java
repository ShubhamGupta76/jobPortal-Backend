package com.job_Portal_Backend.job_portal_backend.dashboard.service;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.CandidateDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.PublicMetricsResponse;
import com.job_Portal_Backend.job_portal_backend.dashboard.dto.RecruiterDashboardResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.mapper.ApplicationMapper;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

        private final ApplicationRepository applicationRepository;
        private final BookmarkRepository bookmarkRepository;
        private final JobRepository jobRepository;
        private final CompanyRepository companyRepository;
        private final UserRepository userRepository;
        private final NotificationService notificationService;
        private final JobMapper jobMapper;
        private final ApplicationMapper applicationMapper;

        public DashboardServiceImpl(
                        ApplicationRepository applicationRepository,
                        BookmarkRepository bookmarkRepository,
                        JobRepository jobRepository,
                        CompanyRepository companyRepository,
                        UserRepository userRepository,
                        NotificationService notificationService,
                        JobMapper jobMapper,
                        ApplicationMapper applicationMapper) {
                this.applicationRepository = applicationRepository;
                this.bookmarkRepository = bookmarkRepository;
                this.jobRepository = jobRepository;
                this.companyRepository = companyRepository;
                this.userRepository = userRepository;
                this.notificationService = notificationService;
                this.jobMapper = jobMapper;
                this.applicationMapper = applicationMapper;
        }

        @Override
        public CandidateDashboardResponse getCandidateDashboard(User user) {
                CandidateDashboardResponse response = new CandidateDashboardResponse();
                response.setAppliedJobs(applicationRepository.countByUserId(user.getId()));
                response.setSavedJobs(bookmarkRepository.countByUserId(user.getId()));
                response.setUnreadNotifications(notificationService.getUnreadNotificationCount(user.getId()));

                List<ApplicationDto> recentApplications = applicationRepository.findByUserId(user.getId())
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

                return response;
        }

        @Override
        public RecruiterDashboardResponse getRecruiterDashboard(User user) {
                RecruiterDashboardResponse response = new RecruiterDashboardResponse();
                response.setTotalJobs(jobRepository.countByRecruiterIdAndNotDeleted(user.getId()));
                response.setTotalApplicants(applicationRepository.countByJobRecruiterId(user.getId()));
                response.setShortlistedCount(applicationRepository
                                .countByJobRecruiterIdAndStatusIgnoreCaseAndNotDeleted(user.getId(), "SHORTLISTED"));

                List<JobDto> recentJobs = jobRepository.findByRecruiterIdAndNotDeleted(user.getId())
                                .stream()
                                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                .limit(5)
                                .map(jobMapper::toDto)
                                .toList();
                response.setRecentJobs(recentJobs);

                List<ApplicationDto> recentApplicants = applicationRepository
                                .findByJobRecruiterId(user.getId(), PageRequest.of(0, 5))
                                .stream()
                                .map(applicationMapper::toDto)
                                .toList();
                response.setRecentApplicants(recentApplicants);

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
}
