package com.job_Portal_Backend.job_portal_backend.notificationpreferences.service;

import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference.Category;

import java.util.Map;

/**
 * Default channel state per category when a user has no explicit {@code NotificationPreference}
 * row. These defaults were chosen by auditing actual current behavior (see info(2).md ENH-016),
 * not by guessing:
 *
 * <ul>
 *   <li>IN_APP defaults to ON for every category, because every one of these categories currently
 *       creates an in-app notification (and pushes it over the existing WebSocket/STOMP queue)
 *       unconditionally today. Defaulting to ON preserves that behavior exactly for every
 *       existing user who never visits the preferences screen.</li>
 *   <li>EMAIL defaults to OFF for every category, because — verified by inspecting every call
 *       site of NotificationService — no category currently triggers an email today. Defaulting
 *       EMAIL to ON would not "preserve current behavior," it would invent new behavior nobody
 *       asked for. Turning EMAIL on is an explicit, informed opt-in the user makes on the
 *       preferences screen, not something switched on for them silently.</li>
 * </ul>
 */
final class NotificationCategoryDefaults {

    private static final boolean DEFAULT_IN_APP = true;
    private static final boolean DEFAULT_EMAIL = false;

    private static final Map<Category, boolean[]> OVERRIDES = Map.of();

    private NotificationCategoryDefaults() {
    }

    static boolean defaultInApp(Category category) {
        boolean[] override = OVERRIDES.get(category);
        return override != null ? override[0] : DEFAULT_IN_APP;
    }

    static boolean defaultEmail(Category category) {
        boolean[] override = OVERRIDES.get(category);
        return override != null ? override[1] : DEFAULT_EMAIL;
    }
}
