package com.job_Portal_Backend.job_portal_backend.notificationpreferences.service;

import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference.Category;

import java.util.Map;

/**
 * Maps the free-text {@code type} strings already used throughout the codebase's
 * {@code NotificationService.sendNotificationToUser(...)} call sites onto a structured
 * {@link Category}. Built by auditing every current call site (see info(2).md ENH-016) rather
 * than guessing — every entry below corresponds to a real, currently-firing notification.
 *
 * <p>Any type string not in this table (including ones from dead/never-called code, or future
 * types nobody has categorized yet) resolves to {@link Category#SYSTEM}, whose default is
 * IN_APP-on/EMAIL-off — the same "always deliver in-app" behavior every notification has today.
 * This means an uncategorized event is never silently dropped by this mapping.
 */
public final class NotificationTypeCategoryMapper {

    private static final Map<String, Category> TYPE_TO_CATEGORY = Map.ofEntries(
            // Applications (applications/service/ApplicationService.java)
            Map.entry("APPLICATION", Category.APPLICATION_UPDATES),
            Map.entry("APPLICATION_STATUS", Category.APPLICATION_UPDATES),
            Map.entry("APPLICATION_UPDATE", Category.APPLICATION_UPDATES),

            // Messaging (messaging/service/MessagingService.java)
            Map.entry("NEW_MESSAGE", Category.MESSAGES),

            // Interviews (interview/service/impl/InterviewSessionServiceImpl.java,
            // service/impl/NotificationServiceImpl.sendInterviewScheduled)
            Map.entry("INTERVIEW_SCHEDULED", Category.INTERVIEW_REMINDERS),
            Map.entry("INTERVIEW_STARTED", Category.INTERVIEW_REMINDERS),

            // Saved search / job alerts (savedsearch/service/SavedSearchService.java)
            Map.entry("JOB_ALERT", Category.JOB_ALERTS),
            Map.entry("NEW_JOB", Category.JOB_ALERTS),

            // Company team management (companyteam/service/CompanyTeamService.java)
            Map.entry("TEAM_INVITATION", Category.RECRUITER_ACTIVITY),
            Map.entry("TEAM_INVITATION_ACCEPTED", Category.RECRUITER_ACTIVITY),

            // Company verification (companyverification/service/CompanyVerificationService.java)
            Map.entry("COMPANY_VERIFICATION_SUBMITTED", Category.COMPANY_VERIFICATION),
            Map.entry("COMPANY_VERIFICATION_DECISION", Category.COMPANY_VERIFICATION),

            // Reports / abuse system (reports/service/ReportService.java)
            Map.entry("REPORT_SUBMITTED", Category.REPORTS),

            // Billing (billing/service/*, ENH-017/SLICE 10)
            Map.entry("SUBSCRIPTION_ACTIVATED", Category.BILLING),
            Map.entry("SUBSCRIPTION_CANCELED", Category.BILLING),
            Map.entry("PAYMENT_SUCCEEDED", Category.BILLING),
            Map.entry("PAYMENT_FAILED", Category.BILLING),
            Map.entry("CREDITS_GRANTED", Category.BILLING),
            Map.entry("CREDITS_LOW", Category.BILLING),

            // Generic/self-confirmation events (bookmarks/service/BookmarkServiceImpl.java, etc.)
            Map.entry("BOOKMARK", Category.SYSTEM),
            Map.entry("WELCOME", Category.SYSTEM));

    private NotificationTypeCategoryMapper() {
    }

    public static Category resolve(String type) {
        if (type == null) {
            return Category.SYSTEM;
        }
        return TYPE_TO_CATEGORY.getOrDefault(type.trim().toUpperCase(), Category.SYSTEM);
    }
}
