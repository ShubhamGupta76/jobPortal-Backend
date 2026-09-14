package com.job_Portal_Backend.job_portal_backend.jobs.service;

import com.job_Portal_Backend.job_portal_backend.billing.service.SubscriptionEntitlementService;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobFilterOptionsResponse;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobCreateRequest;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobFilterRequest;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.SearchSuggestionsResponse;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobUpdateRequest;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.savedsearch.service.SavedSearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationRepository applicationRepository;
    private final JobMapper jobMapper;
    private final SavedSearchService savedSearchService;
    private final SubscriptionEntitlementService entitlementService;

    public JobService(
            JobRepository jobRepository,
            CompanyRepository companyRepository,
            ApplicationRepository applicationRepository,
            JobMapper jobMapper,
            SavedSearchService savedSearchService,
            SubscriptionEntitlementService entitlementService) {
        this.jobRepository = jobRepository;
        this.companyRepository = companyRepository;
        this.applicationRepository = applicationRepository;
        this.jobMapper = jobMapper;
        this.savedSearchService = savedSearchService;
        this.entitlementService = entitlementService;
    }

    public JobDto createJob(JobCreateRequest request, User recruiter) {
        Company company = companyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        // Entitlement is determined entirely server-side from the company's actual subscription
        // (never a client-supplied plan). The seeded FREE plan has no job limit, so this is a
        // no-op for every company that isn't on a plan with an explicit, real limit.
        if (!entitlementService.canPostJob(company.getId())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Your plan's active job posting limit (" + entitlementService.maxActiveJobs(company.getId())
                            + ") has been reached. Upgrade your plan to post more jobs.");
        }

        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setDepartment(request.getDepartment());
        job.setLocation(request.getLocation());
        job.setWorkplaceType(request.getWorkplaceType());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setMinSalary(request.getMinSalary());
        job.setMaxSalary(request.getMaxSalary());
        job.setSkills(request.getSkills());
        job.setStatus("ACTIVE");
        job.setIsDeleted(false);
        job.setCompany(company);
        job.setRecruiter(recruiter);

        job = jobRepository.save(job);
        try {
            savedSearchService.processNewJob(job);
        } catch (RuntimeException alertFailure) {
            log.warn("Saved-search alert delivery failed for job {}", job.getId(), alertFailure);
        }
        return jobMapper.toDto(job);
    }

    public JobDto updateJob(Long jobId, JobUpdateRequest request, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to update this job");
        }

        if (request.getTitle() != null)
            job.setTitle(request.getTitle());
        if (request.getDescription() != null)
            job.setDescription(request.getDescription());
        if (request.getDepartment() != null)
            job.setDepartment(request.getDepartment());
        if (request.getLocation() != null)
            job.setLocation(request.getLocation());
        if (request.getWorkplaceType() != null)
            job.setWorkplaceType(request.getWorkplaceType());
        if (request.getJobType() != null)
            job.setJobType(request.getJobType());
        if (request.getExperienceLevel() != null)
            job.setExperienceLevel(request.getExperienceLevel());
        if (request.getMinSalary() != null)
            job.setMinSalary(request.getMinSalary());
        if (request.getMaxSalary() != null)
            job.setMaxSalary(request.getMaxSalary());
        if (request.getSkills() != null)
            job.setSkills(request.getSkills());
        if (request.getCompanyId() != null) {
            Company company = companyRepository.findById(request.getCompanyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Company not found"));
            job.setCompany(company);
        }

        job = jobRepository.save(job);
        return jobMapper.toDto(job);
    }

    public void deleteJob(Long jobId, User recruiter) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new RuntimeException("Unauthorized to delete this job");
        }

        job.setIsDeleted(true);
        jobRepository.save(job);
    }

    public List<JobDto> getJobs(JobFilterRequest filter) {
        return getJobs(filter, null);
    }

    public List<JobDto> getJobs(JobFilterRequest filter, User candidate) {
        final Sort sort = "desc".equalsIgnoreCase(filter.getSortDir())
                ? Sort.by(filter.getSortBy()).descending()
                : Sort.by(filter.getSortBy()).ascending();
        final String sortProperty = sort.iterator().next().getProperty();
        final Sort.Direction sortDirection = sort.iterator().next().getDirection();

        List<Job> jobs = jobRepository.findJobsWithFilters(
                filter.getLocation(),
                filter.getMinSalary(),
                filter.getMaxSalary(),
                filter.getJobType(),
                filter.getExperienceLevel(),
                filter.getKeyword());

        return jobs.stream()
                .sorted((first, second) -> {
                    if (sort.isSorted()) {
                        int comparison = switch (sortProperty) {
                            case "title" -> compareNullableStrings(first.getTitle(), second.getTitle());
                            case "location" -> compareNullableStrings(first.getLocation(), second.getLocation());
                            case "updatedAt" -> compareNullableDateTimes(first.getUpdatedAt(), second.getUpdatedAt());
                            case "minSalary" -> compareNullableDoubles(first.getMinSalary(), second.getMinSalary());
                            case "maxSalary" -> compareNullableDoubles(first.getMaxSalary(), second.getMaxSalary());
                            case "createdAt" -> compareNullableDateTimes(first.getCreatedAt(), second.getCreatedAt());
                            default -> compareNullableDateTimes(first.getCreatedAt(), second.getCreatedAt());
                        };

                        return sortDirection.isDescending() ? -comparison : comparison;
                    }

                    return compareNullableDateTimes(first.getCreatedAt(), second.getCreatedAt());
                })
                .skip((long) filter.getPage() * filter.getSize())
                .limit(filter.getSize())
                .map(job -> candidate == null ? jobMapper.toDto(job) : jobMapper.toDto(job, candidate))
                .collect(Collectors.toList());
    }

    public JobDto getJobById(Long jobId) {
            return getJobById(jobId, null);
            }

            public JobDto getJobById(Long jobId, User candidate) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
            return candidate == null ? jobMapper.toDto(job) : jobMapper.toDto(job, candidate);
    }

    public List<JobDto> getJobsByRecruiter(User recruiter) {
        List<Job> jobs = jobRepository.findByRecruiterIdAndNotDeleted(recruiter.getId());
        return jobs.stream()
                .map(job -> {
                    JobDto dto = jobMapper.toDto(job);
                    dto.setApplicationCount(applicationRepository.countByJobIdAndNotDeleted(job.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public int archiveOldJobs(User recruiter, int olderThanDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(Math.max(olderThanDays, 1));
        List<Job> jobs = jobRepository.findByRecruiterIdAndNotDeleted(recruiter.getId());

        List<Job> jobsToArchive = jobs.stream()
                .filter(job -> job.getCreatedAt() != null && job.getCreatedAt().isBefore(threshold))
                .filter(job -> job.getStatus() == null || !"CLOSED".equalsIgnoreCase(job.getStatus()))
                .toList();

        jobsToArchive.forEach(job -> job.setStatus("CLOSED"));
        if (!jobsToArchive.isEmpty()) {
            jobRepository.saveAll(jobsToArchive);
        }

        return jobsToArchive.size();
    }

    public SearchSuggestionsResponse getSearchSuggestions(String keyword) {
        SearchSuggestionsResponse response = new SearchSuggestionsResponse();
        if (keyword == null || keyword.isBlank()) {
            response.setSuggestions(List.of());
            return response;
        }

        List<String> suggestions = new java.util.ArrayList<>();
        suggestions.addAll(jobRepository.findMatchingTitles(keyword, PageRequest.of(0, 5)));
        suggestions.addAll(jobRepository.findMatchingCompanyNames(keyword, PageRequest.of(0, 5)));
        response.setSuggestions(suggestions.stream().distinct().limit(8).toList());
        return response;
    }

    public JobFilterOptionsResponse getFilterOptions() {
        JobFilterOptionsResponse response = new JobFilterOptionsResponse();
        response.setLocations(jobRepository.findDistinctLocations());
        response.setJobTypes(jobRepository.findDistinctJobTypes());
        response.setExperienceLevels(jobRepository.findDistinctExperienceLevels());
        response.setMinSalary(jobRepository.findMinSalary());
        response.setMaxSalary(jobRepository.findMaxSalary());
        return response;
    }

    private int compareNullableStrings(String first, String second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return first.compareToIgnoreCase(second);
    }

    private int compareNullableDateTimes(java.time.LocalDateTime first, java.time.LocalDateTime second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return first.compareTo(second);
    }

    private int compareNullableDoubles(Double first, Double second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return Double.compare(first, second);
    }
}
