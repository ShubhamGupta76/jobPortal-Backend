package com.job_Portal_Backend.job_portal_backend.profileanalytics.service;

import com.job_Portal_Backend.job_portal_backend.entity.ApplicationStatusHistory;
import com.job_Portal_Backend.job_portal_backend.entity.Bookmark;
import com.job_Portal_Backend.job_portal_backend.entity.SavedSearchDelivery;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.Message;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageRepository;
import com.job_Portal_Backend.job_portal_backend.profileanalytics.dto.ActivityEventDto;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationStatusHistoryRepository;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.SavedSearchDeliveryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CandidateActivityService {

    private final ApplicationStatusHistoryRepository applicationStatusHistoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SavedSearchDeliveryRepository savedSearchDeliveryRepository;
    private final MessageRepository messageRepository;

    public CandidateActivityService(ApplicationStatusHistoryRepository applicationStatusHistoryRepository,
            BookmarkRepository bookmarkRepository,
            SavedSearchDeliveryRepository savedSearchDeliveryRepository,
            MessageRepository messageRepository) {
        this.applicationStatusHistoryRepository = applicationStatusHistoryRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.savedSearchDeliveryRepository = savedSearchDeliveryRepository;
        this.messageRepository = messageRepository;
    }

    public List<ActivityEventDto> getActivity(User user, String typeFilter, int page, int size) {
        List<ActivityEventDto> events = new ArrayList<>();

        for (ApplicationStatusHistory history : applicationStatusHistoryRepository
                .findTop50ByApplication_UserIdOrderByCreatedAtDesc(user.getId())) {
            boolean isInitial = history.getPreviousStatus() == null;
            events.add(new ActivityEventDto(
                    isInitial ? "APPLICATION_SUBMITTED" : "APPLICATION_STATUS_CHANGED",
                    isInitial
                            ? "Applied to " + history.getApplication().getJob().getTitle()
                            : "Application for " + history.getApplication().getJob().getTitle()
                                    + " moved to " + formatLabel(history.getStatus().name()),
                    history.getNote(),
                    history.getCreatedAt(),
                    "APPLICATION",
                    history.getApplication().getId()));
        }

        for (Bookmark bookmark : bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())) {
            events.add(new ActivityEventDto(
                    "JOB_SAVED",
                    "Saved " + bookmark.getJob().getTitle(),
                    null,
                    bookmark.getCreatedAt(),
                    "JOB",
                    bookmark.getJob().getId()));
        }

        for (SavedSearchDelivery delivery : savedSearchDeliveryRepository
                .findTop50BySavedSearch_UserIdOrderByCreatedAtDesc(user.getId())) {
            events.add(new ActivityEventDto(
                    "JOB_ALERT_MATCHED",
                    "\"" + delivery.getSavedSearch().getName() + "\" alert matched " + delivery.getJob().getTitle(),
                    null,
                    delivery.getCreatedAt(),
                    "JOB",
                    delivery.getJob().getId()));
        }

        for (Message message : messageRepository.findTop50ReceivedByUserId(user.getId(), PageRequest.of(0, 50))) {
            events.add(new ActivityEventDto(
                    "MESSAGE_RECEIVED",
                    "New message from " + fullName(message.getSender()),
                    truncate(message.getContent()),
                    message.getCreatedAt(),
                    "CONVERSATION",
                    message.getConversation().getId()));
        }

        List<ActivityEventDto> filtered = events.stream()
                .filter(event -> typeFilter == null || typeFilter.isBlank() || "ALL".equalsIgnoreCase(typeFilter)
                        || event.getType().equalsIgnoreCase(typeFilter))
                .sorted(Comparator.comparing(ActivityEventDto::getTimestamp).reversed())
                .toList();

        int from = Math.min(page * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        return filtered.subList(from, to);
    }

    private String fullName(User user) {
        String name = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 140 ? value.substring(0, 140) + "..." : value;
    }

    private String formatLabel(String value) {
        if (value == null) return "";
        String lower = value.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
