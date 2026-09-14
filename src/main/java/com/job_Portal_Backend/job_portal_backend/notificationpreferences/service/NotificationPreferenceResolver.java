package com.job_Portal_Backend.job_portal_backend.notificationpreferences.service;

import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference;
import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference.Category;
import com.job_Portal_Backend.job_portal_backend.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves the effective (row-or-default) preference for a single (user, category) pair.
 * This is the one place that merges an explicit {@link NotificationPreference} row with
 * {@link NotificationCategoryDefaults} — both {@link com.job_Portal_Backend.job_portal_backend.service.impl.NotificationServiceImpl}
 * (to decide whether to deliver) and the preferences API (to render current settings) go
 * through this resolver so the two can never disagree about what "current" means.
 */
@Component
public class NotificationPreferenceResolver {

    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public NotificationPreferenceResolver(NotificationPreferenceRepository notificationPreferenceRepository) {
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    public Effective resolve(Long userId, Category category) {
        return notificationPreferenceRepository.findByUserIdAndCategory(userId, category)
                .map(pref -> new Effective(
                        Boolean.TRUE.equals(pref.getInAppEnabled()),
                        Boolean.TRUE.equals(pref.getEmailEnabled())))
                .orElseGet(() -> new Effective(
                        NotificationCategoryDefaults.defaultInApp(category),
                        NotificationCategoryDefaults.defaultEmail(category)));
    }

    public record Effective(boolean inAppEnabled, boolean emailEnabled) {
    }
}
