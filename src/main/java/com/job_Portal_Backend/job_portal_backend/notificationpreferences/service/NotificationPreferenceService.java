package com.job_Portal_Backend.job_portal_backend.notificationpreferences.service;

import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference;
import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference.Category;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.notificationpreferences.dto.NotificationPreferenceDto;
import com.job_Portal_Backend.job_portal_backend.repository.NotificationPreferenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the CRUD side of notification preferences (the settings screen). The actual
 * enforcement — deciding whether a given notification is delivered in-app or emailed — lives
 * in {@link NotificationPreferenceResolver}, consumed directly by
 * {@link com.job_Portal_Backend.job_portal_backend.service.impl.NotificationServiceImpl}, so
 * there is exactly one source of truth for "what does this user's preference currently mean."
 */
@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final NotificationPreferenceResolver resolver;

    public NotificationPreferenceService(NotificationPreferenceRepository notificationPreferenceRepository,
            NotificationPreferenceResolver resolver) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.resolver = resolver;
    }

    /** Always returns every known category (row-backed or default) so the settings screen never needs to guess. */
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getPreferences(User user) {
        return List.of(Category.values()).stream()
                .map(category -> {
                    NotificationPreferenceResolver.Effective effective = resolver.resolve(user.getId(), category);
                    return new NotificationPreferenceDto(category.name(), effective.inAppEnabled(), effective.emailEnabled());
                })
                .toList();
    }

    /**
     * Partial upsert: only categories present in {@code updates} are created/changed. Categories
     * the caller omits are left exactly as they were (row-backed or default) — a role-filtered
     * settings screen that only shows/saves a subset of categories can never reset ones it never
     * displayed. This also means a single PUT can be retried safely (idempotent per category).
     */
    @Transactional
    public List<NotificationPreferenceDto> updatePreferences(User user, List<NotificationPreferenceDto> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one preference update is required.");
        }

        Map<Category, NotificationPreference> existingByCategory = notificationPreferenceRepository
                .findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(NotificationPreference::getCategory, pref -> pref));

        for (NotificationPreferenceDto update : updates) {
            Category category = parseCategory(update.getCategory());
            NotificationPreference preference = existingByCategory.get(category);
            if (preference == null) {
                preference = new NotificationPreference();
                preference.setUser(user);
                preference.setCategory(category);
            }
            preference.setInAppEnabled(update.isInAppEnabled());
            preference.setEmailEnabled(update.isEmailEnabled());
            notificationPreferenceRepository.save(preference);
        }

        return getPreferences(user);
    }

    private Category parseCategory(String value) {
        try {
            return Category.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification category: " + value);
        }
    }
}
