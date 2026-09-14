package com.job_Portal_Backend.job_portal_backend.recommendations.service;

import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.DismissedJob;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.jobs.service.JobMatchingService;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.recommendations.dto.RecommendedJobDto;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.DismissedJobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RecommendationServiceTest {

    private final JobRepository jobRepository = mock(JobRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final BookmarkRepository bookmarkRepository = mock(BookmarkRepository.class);
    private final DismissedJobRepository dismissedJobRepository = mock(DismissedJobRepository.class);
    private final CompanyVerificationRepository companyVerificationRepository = mock(CompanyVerificationRepository.class);

    // JobMapper is a real instance wrapping the real JobMatchingService, so the recommendation
    // engine is exercised against the actual base-scoring algorithm, not a mock of it.
    private final JobMapper jobMapper = new JobMapper(new JobMatchingService(), companyVerificationRepository);

    private final RecommendationService service = new RecommendationService(
            jobRepository, jobMapper, applicationRepository, bookmarkRepository, dismissedJobRepository);

    private User candidateWith(String skills, String experienceLevel, String location, String preferredJobType, String preferredWorkplaceType) {
        User user = new User();
        user.setId(1L);
        user.setEmail("candidate@example.com");
        user.setSkills(skills);
        user.setExperienceLevel(experienceLevel);
        user.setLocation(location);
        user.setPreferredJobType(preferredJobType);
        user.setPreferredWorkplaceType(preferredWorkplaceType);
        return user;
    }

    private Job job(Long id, String title, String skills, String experienceLevel, String location, LocalDateTime createdAt) {
        Job job = new Job();
        job.setId(id);
        job.setTitle(title);
        job.setSkills(skills);
        job.setExperienceLevel(experienceLevel);
        job.setLocation(location);
        job.setStatus("ACTIVE");
        job.setIsDeleted(false);
        job.setCreatedAt(createdAt);
        Company company = new Company();
        company.setId(100L + id);
        company.setName("Company " + id);
        company.setIsDeleted(false);
        job.setCompany(company);
        return job;
    }

    private void noExclusions() {
        when(applicationRepository.findByUserIdAndNotDeleted(1L)).thenReturn(List.of());
        when(bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(dismissedJobRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(companyVerificationRepository.findByCompanyId(any())).thenReturn(Optional.empty());
    }

    @Test
    void strongSkillMatchRanksHigher() {
        User candidate = candidateWith("Java, Spring, PostgreSQL", null, null, null, null);
        noExclusions();

        Job strongMatch = job(1L, "Backend Engineer", "Java, Spring, PostgreSQL", null, null, LocalDateTime.now().minusDays(30));
        Job weakMatch = job(2L, "Frontend Engineer", "React, CSS, HTML", null, null, LocalDateTime.now().minusDays(30));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(weakMatch, strongMatch));

        Page<RecommendedJobDto> page = service.getRecommendations(candidate, 0, 20, null);

        assertEquals(1L, page.getContent().get(0).getJob().getId());
        assertTrue(page.getContent().get(0).getReasonCodes().contains(RecommendationReason.MATCH_SKILLS));
    }

    @Test
    void experienceAndLocationReasonCodesAppearWhenMatched() {
        User candidate = candidateWith("Java", "SENIOR", "Bangalore", null, null);
        noExclusions();

        Job matching = job(1L, "Senior Engineer", "Java", "SENIOR", "Bangalore", LocalDateTime.now().minusDays(30));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(matching));

        Page<RecommendedJobDto> page = service.getRecommendations(candidate, 0, 20, null);

        List<String> reasons = page.getContent().get(0).getReasonCodes();
        assertTrue(reasons.contains(RecommendationReason.MATCH_EXPERIENCE));
        assertTrue(reasons.contains(RecommendationReason.MATCH_LOCATION));
    }

    @Test
    void alreadyAppliedJobIsExcluded() {
        User candidate = candidateWith("Java", null, null, null, null);
        Job appliedJob = job(1L, "Backend Engineer", "Java", null, null, LocalDateTime.now().minusDays(30));

        Application application = new Application();
        application.setJob(appliedJob);
        when(applicationRepository.findByUserIdAndNotDeleted(1L)).thenReturn(List.of(application));
        when(bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(dismissedJobRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        Job otherJob = job(2L, "Other role", "Java", null, null, LocalDateTime.now().minusDays(30));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(appliedJob, otherJob));

        Page<RecommendedJobDto> page = service.getRecommendations(candidate, 0, 20, null);

        assertEquals(1, page.getContent().size());
        assertEquals(2L, page.getContent().get(0).getJob().getId());
    }

    @Test
    void dismissedJobIsExcluded() {
        User candidate = candidateWith("Java", null, null, null, null);
        Job dismissedJobEntity = job(1L, "Backend Engineer", "Java", null, null, LocalDateTime.now().minusDays(30));

        DismissedJob dismissed = new DismissedJob();
        dismissed.setJob(dismissedJobEntity);
        when(applicationRepository.findByUserIdAndNotDeleted(1L)).thenReturn(List.of());
        when(bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(dismissedJobRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(dismissed));

        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(dismissedJobEntity));

        Page<RecommendedJobDto> page = service.getRecommendations(candidate, 0, 20, null);

        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void verifiedCompanyAddsBoostAndReasonCode() {
        User candidate = candidateWith(null, null, null, null, null);
        noExclusions();

        Job verifiedJob = job(1L, "Role at verified company", null, null, null, LocalDateTime.now().minusDays(30));
        com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification verification =
                new com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification();
        verification.setStatus(com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification.VerificationStatus.VERIFIED);
        when(companyVerificationRepository.findByCompanyId(verifiedJob.getCompany().getId())).thenReturn(Optional.of(verification));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(verifiedJob));

        Page<RecommendedJobDto> page = service.getRecommendations(candidate, 0, 20, null);

        RecommendedJobDto dto = page.getContent().get(0);
        assertTrue(dto.getReasonCodes().contains(RecommendationReason.VERIFIED_COMPANY));
        assertTrue(dto.getFinalScore() >= RecommendationService.VERIFIED_COMPANY_BOOST);
    }

    @Test
    void recommendationsAreDeterministicAcrossCalls() {
        User candidate = candidateWith("Java, Spring", "MID", "Remote", "FULL_TIME", "REMOTE");
        noExclusions();

        Job jobA = job(1L, "Job A", "Java, Spring", "MID", "Remote", LocalDateTime.now().minusDays(2));
        Job jobB = job(2L, "Job B", "Java", "MID", "Remote", LocalDateTime.now().minusDays(10));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(jobA, jobB));

        Page<RecommendedJobDto> first = service.getRecommendations(candidate, 0, 20, null);
        Page<RecommendedJobDto> second = service.getRecommendations(candidate, 0, 20, null);

        List<Long> firstOrder = first.getContent().stream().map(item -> item.getJob().getId()).toList();
        List<Long> secondOrder = second.getContent().stream().map(item -> item.getJob().getId()).toList();
        assertEquals(firstOrder, secondOrder);
    }

    @Test
    void paginationSlicesRankedResultsCorrectly() {
        User candidate = candidateWith(null, null, null, null, null);
        noExclusions();

        List<Job> jobs = List.of(
                job(1L, "Job 1", null, null, null, LocalDateTime.now()),
                job(2L, "Job 2", null, null, null, LocalDateTime.now()),
                job(3L, "Job 3", null, null, null, LocalDateTime.now()));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(jobs);

        Page<RecommendedJobDto> firstPage = service.getRecommendations(candidate, 0, 2, null);
        Page<RecommendedJobDto> secondPage = service.getRecommendations(candidate, 1, 2, null);

        assertEquals(2, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
        assertEquals(3, firstPage.getTotalElements());
    }

    @Test
    void dismissAlwaysScopesToTheCallingCandidate() {
        User candidateOne = candidateWith(null, null, null, null, null);
        Job targetJob = job(5L, "Some role", null, null, null, LocalDateTime.now());
        when(jobRepository.findById(5L)).thenReturn(Optional.of(targetJob));
        when(dismissedJobRepository.existsByUserIdAndJobId(1L, 5L)).thenReturn(false);

        service.dismiss(candidateOne, 5L);

        verify(dismissedJobRepository).save(argThat(dismissal ->
                dismissal.getUser().getId().equals(1L) && dismissal.getJob().getId().equals(5L)));
    }

    @Test
    void emptyJobPoolReturnsEmptyPageWithoutError() {
        User candidate = candidateWith(null, null, null, null, null);
        noExclusions();
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of());

        Page<RecommendedJobDto> page = service.getRecommendations(candidate, 0, 20, null);

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void candidateWithNoProfileDataStillScoresWithoutError() {
        User bareCandidate = new User();
        bareCandidate.setId(1L);
        bareCandidate.setEmail("bare@example.com");
        noExclusions();

        Job job = job(1L, "Any role", "Java", "MID", "Remote", LocalDateTime.now());
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, null)).thenReturn(List.of(job));

        Page<RecommendedJobDto> page = service.getRecommendations(bareCandidate, 0, 20, null);

        assertEquals(1, page.getContent().size());
        assertTrue(page.getContent().get(0).getFinalScore() >= 0);
    }
}
