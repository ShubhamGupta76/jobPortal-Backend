package com.job_Portal_Backend.job_portal_backend.profileanalytics.service;

import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.ApplicationStatusHistory;
import com.job_Portal_Backend.job_portal_backend.entity.Bookmark;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageRepository;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.dto.ActivityEventDto;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationStatusHistoryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.SavedSearchDeliveryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class CandidateActivityServiceTest {

    private final ApplicationStatusHistoryRepository historyRepository = mock(ApplicationStatusHistoryRepository.class);
    private final BookmarkRepository bookmarkRepository = mock(BookmarkRepository.class);
    private final SavedSearchDeliveryRepository deliveryRepository = mock(SavedSearchDeliveryRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);

    private final CandidateActivityService service = new CandidateActivityService(
            historyRepository, bookmarkRepository, deliveryRepository, messageRepository);

    @Test
    void mergesAndSortsEventsFromAllSourcesNewestFirst() {
        User candidate = user(1L);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Backend Engineer");

        Application application = new Application();
        application.setId(20L);
        application.setJob(job);

        ApplicationStatusHistory submitted = new ApplicationStatusHistory();
        submitted.setApplication(application);
        submitted.setPreviousStatus(null);
        submitted.setStatus(Application.ApplicationStatus.APPLIED);
        submitted.setCreatedAt(LocalDateTime.now().minusDays(2));

        Bookmark bookmark = new Bookmark();
        bookmark.setJob(job);
        bookmark.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(historyRepository.findTop50ByApplication_UserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(submitted));
        when(bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(bookmark));
        when(deliveryRepository.findTop50BySavedSearch_UserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(messageRepository.findTop50ReceivedByUserId(anyLong(), any())).thenReturn(List.of());

        List<ActivityEventDto> events = service.getActivity(candidate, "ALL", 0, 20);

        assertEquals(2, events.size());
        assertEquals("JOB_SAVED", events.get(0).getType());
        assertEquals("APPLICATION_SUBMITTED", events.get(1).getType());
    }

    @Test
    void filtersByRequestedType() {
        User candidate = user(1L);

        Job job = new Job();
        job.setId(10L);
        job.setTitle("Backend Engineer");
        Bookmark bookmark = new Bookmark();
        bookmark.setJob(job);
        bookmark.setCreatedAt(LocalDateTime.now());

        when(historyRepository.findTop50ByApplication_UserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(bookmark));
        when(deliveryRepository.findTop50BySavedSearch_UserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(messageRepository.findTop50ReceivedByUserId(anyLong(), any())).thenReturn(List.of());

        List<ActivityEventDto> filteredOut = service.getActivity(candidate, "MESSAGE_RECEIVED", 0, 20);
        List<ActivityEventDto> filteredIn = service.getActivity(candidate, "JOB_SAVED", 0, 20);

        assertEquals(0, filteredOut.size());
        assertEquals(1, filteredIn.size());
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("Candidate");
        user.setEmail("candidate" + id + "@example.com");
        return user;
    }
}
