package com.job_Portal_Backend.job_portal_backend.savedsearch.service;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.SavedSearch;
import com.job_Portal_Backend.job_portal_backend.entity.SavedSearchDelivery;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobFilterRequest;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.SavedSearchDeliveryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.SavedSearchRepository;
import com.job_Portal_Backend.job_portal_backend.savedsearch.dto.SavedSearchRequest;
import com.job_Portal_Backend.job_portal_backend.savedsearch.dto.SavedSearchResponse;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final SavedSearchDeliveryRepository deliveryRepository;
    private final JobRepository jobRepository;
    private final NotificationService notificationService;

    public SavedSearchService(SavedSearchRepository savedSearchRepository,
                              SavedSearchDeliveryRepository deliveryRepository,
                              JobRepository jobRepository,
                              NotificationService notificationService) {
        this.savedSearchRepository = savedSearchRepository;
        this.deliveryRepository = deliveryRepository;
        this.jobRepository = jobRepository;
        this.notificationService = notificationService;
    }

    public List<SavedSearchResponse> getForUser(User user) {
        return savedSearchRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public SavedSearchResponse create(SavedSearchRequest request, User user) {
        validateRequest(request);
        SavedSearch search = new SavedSearch();
        search.setUser(user);
        copyRequest(request, search);
        return toResponse(savedSearchRepository.save(search));
    }

    public SavedSearchResponse update(Long id, SavedSearchRequest request, User user) {
        validateRequest(request);
        SavedSearch search = ownedSearch(id, user);
        copyRequest(request, search);
        return toResponse(savedSearchRepository.save(search));
    }

    public void delete(Long id, User user) {
        savedSearchRepository.delete(ownedSearch(id, user));
    }

    public SavedSearchResponse setEnabled(Long id, boolean enabled, User user) {
        SavedSearch search = ownedSearch(id, user);
        search.setEnabled(enabled);
        return toResponse(savedSearchRepository.save(search));
    }

    public void processNewJob(Job job) {
        if (job == null || Boolean.TRUE.equals(job.getIsDeleted()) || !"ACTIVE".equalsIgnoreCase(job.getStatus())) {
            return;
        }

        for (SavedSearch search : savedSearchRepository.findByEnabledTrue()) {
            if (!matches(search, job)) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            search.setLastMatchedAt(now);
            if (deliveryRepository.existsBySavedSearchIdAndJobId(search.getId(), job.getId())
                    || !withinFrequency(search, now)) {
                savedSearchRepository.save(search);
                continue;
            }

            SavedSearchDelivery delivery = new SavedSearchDelivery();
            delivery.setSavedSearch(search);
            delivery.setJob(job);
            try {
                deliveryRepository.saveAndFlush(delivery);
            } catch (DataIntegrityViolationException duplicateDelivery) {
                continue;
            }

            search.setLastDeliveredAt(now);
            savedSearchRepository.save(search);
            notificationService.sendNotificationToUser(
                    search.getUser(),
                    "New job matches your search",
                    job.getTitle() + " matches saved search \"" + search.getName() + "\".",
                    "JOB_ALERT",
                    "{\"jobId\":" + job.getId() + ",\"savedSearchId\":" + search.getId() + "}");
        }
    }

    private boolean matches(SavedSearch search, Job job) {
        JobFilterRequest filter = new JobFilterRequest();
        filter.setLocation(search.getLocation());
        filter.setMinSalary(search.getMinSalary());
        filter.setMaxSalary(search.getMaxSalary());
        filter.setJobType(search.getJobType());
        filter.setExperienceLevel(search.getExperienceLevel());
        filter.setKeyword(search.getKeyword());
        boolean baseMatch = jobRepository.findJobsWithFilters(
                        filter.getLocation(), filter.getMinSalary(), filter.getMaxSalary(),
                        filter.getJobType(), filter.getExperienceLevel(), filter.getKeyword())
                .stream()
                .anyMatch(candidate -> candidate.getId().equals(job.getId()));
        if (!baseMatch || isBlank(search.getSkills())) {
            return baseMatch && (isBlank(search.getWorkplaceType())
                || normalize(search.getWorkplaceType()).equals(normalize(job.getWorkplaceType())));
        }
        String jobSkills = normalize(job.getSkills());
        boolean skillsMatch = Arrays.stream(search.getSkills().split(","))
                .map(this::normalize)
                .filter(value -> !value.isBlank())
                .allMatch(jobSkills::contains);
        return skillsMatch && (isBlank(search.getWorkplaceType())
            || normalize(search.getWorkplaceType()).equals(normalize(job.getWorkplaceType())));
    }

    private boolean withinFrequency(SavedSearch search, LocalDateTime now) {
        if (search.getLastDeliveredAt() == null || "IMMEDIATE".equalsIgnoreCase(search.getFrequency())) {
            return true;
        }
        Duration elapsed = Duration.between(search.getLastDeliveredAt(), now);
        long windowHours = "WEEKLY".equalsIgnoreCase(search.getFrequency()) ? 24 * 7 : 24;
        return elapsed.toHours() >= windowHours;
    }

    private SavedSearch ownedSearch(Long id, User user) {
        SavedSearch search = savedSearchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Saved search not found"));
        if (!search.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to manage this saved search");
        }
        return search;
    }

    private void copyRequest(SavedSearchRequest request, SavedSearch search) {
        search.setName(request.getName().trim());
        search.setKeyword(trimToNull(request.getKeyword()));
        search.setLocation(trimToNull(request.getLocation()));
        search.setSkills(trimToNull(request.getSkills()));
        search.setExperienceLevel(trimToNull(request.getExperienceLevel()));
        search.setJobType(trimToNull(request.getJobType()));
        search.setWorkplaceType(trimToNull(request.getWorkplaceType()));
        search.setMinSalary(request.getMinSalary());
        search.setMaxSalary(request.getMaxSalary());
        search.setFrequency(normalizeFrequency(request.getFrequency()));
        search.setEnabled(request.getEnabled() == null || request.getEnabled());
    }

    private void validateRequest(SavedSearchRequest request) {
        if (request.getMinSalary() != null && request.getMaxSalary() != null
                && request.getMinSalary() > request.getMaxSalary()) {
            throw new RuntimeException("Minimum salary cannot exceed maximum salary");
        }
        normalizeFrequency(request.getFrequency());
    }

    private String normalizeFrequency(String frequency) {
        String normalized = isBlank(frequency) ? "IMMEDIATE" : frequency.trim().toUpperCase(Locale.ROOT);
        if (!List.of("IMMEDIATE", "DAILY", "WEEKLY").contains(normalized)) {
            throw new RuntimeException("Frequency must be IMMEDIATE, DAILY, or WEEKLY");
        }
        return normalized;
    }

    private SavedSearchResponse toResponse(SavedSearch search) {
        SavedSearchResponse response = new SavedSearchResponse();
        response.setId(search.getId());
        response.setName(search.getName());
        response.setKeyword(search.getKeyword());
        response.setLocation(search.getLocation());
        response.setSkills(search.getSkills());
        response.setExperienceLevel(search.getExperienceLevel());
        response.setJobType(search.getJobType());
        response.setWorkplaceType(search.getWorkplaceType());
        response.setMinSalary(search.getMinSalary());
        response.setMaxSalary(search.getMaxSalary());
        response.setFrequency(search.getFrequency());
        response.setEnabled(search.getEnabled());
        response.setLastMatchedAt(search.getLastMatchedAt());
        response.setLastDeliveredAt(search.getLastDeliveredAt());
        response.setCreatedAt(search.getCreatedAt());
        response.setUpdatedAt(search.getUpdatedAt());
        return response;
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}