package com.job_Portal_Backend.job_portal_backend.savedsearch.service;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.SavedSearch;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.SavedSearchDeliveryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.SavedSearchRepository;
import com.job_Portal_Backend.job_portal_backend.savedsearch.dto.SavedSearchRequest;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SavedSearchServiceTest {

    private final SavedSearchRepository searchRepository = mock(SavedSearchRepository.class);
    private final SavedSearchDeliveryRepository deliveryRepository = mock(SavedSearchDeliveryRepository.class);
    private final JobRepository jobRepository = mock(JobRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final SavedSearchService service = new SavedSearchService(
            searchRepository, deliveryRepository, jobRepository, notificationService);

    @Test
    void sendsOneScopedAlertForAMatchingJob() {
        User candidate = user(7L);
        SavedSearch search = new SavedSearch();
        search.setId(11L);
        search.setUser(candidate);
        search.setName("Remote Java");
        search.setKeyword("Java");
        search.setSkills("Java, Spring");
        search.setEnabled(true);
        search.setFrequency("IMMEDIATE");

        Job job = new Job();
        job.setId(21L);
        job.setTitle("Java Engineer");
        job.setDescription("Build Spring services");
        job.setSkills("Java, Spring, PostgreSQL");
        job.setStatus("ACTIVE");
        job.setIsDeleted(false);

        when(searchRepository.findByEnabledTrue()).thenReturn(List.of(search));
        when(jobRepository.findJobsWithFilters(null, null, null, null, null, "Java")).thenReturn(List.of(job));
        when(deliveryRepository.existsBySavedSearchIdAndJobId(11L, 21L)).thenReturn(false);

        service.processNewJob(job);

        verify(deliveryRepository).saveAndFlush(any());
        verify(notificationService).sendNotificationToUser(
                eq(candidate), eq("New job matches your search"), anyString(), eq("JOB_ALERT"), anyString());
    }

    @Test
    void refusesToManageAnotherCandidatesSearch() {
        SavedSearch search = new SavedSearch();
        search.setId(11L);
        search.setUser(user(7L));
        when(searchRepository.findById(11L)).thenReturn(java.util.Optional.of(search));

        assertThrows(RuntimeException.class, () -> service.setEnabled(11L, false, user(8L)));
        verifyNoInteractions(notificationService);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("Candidate");
        return user;
    }
}