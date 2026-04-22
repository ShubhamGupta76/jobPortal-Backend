package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.dto.NotificationDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationService {

    void sendNotificationToUser(Long userId, String type, String title, String message, String data);

    void sendNotificationToUsers(List<Long> userIds, String type, String title, String message, String data);

    void sendNotificationToUser(User user, String title, String message, String type);

    void sendNotificationToUsers(List<User> users, String title, String message, String type);

    void sendBulkNotification(String title, String message, String type);

    List<NotificationDto> getUserNotifications(Long userId, int page, int size);

    List<NotificationDto> getUserNotifications(User user, boolean unreadOnly);

    NotificationDto markNotificationAsRead(Long userId, Long notificationId);

    NotificationDto markNotificationAsRead(Long notificationId, User user);

    void markAllNotificationsAsRead(Long userId);

    void markAllNotificationsAsRead(User user);

    long getUnreadNotificationCount(Long userId);

    long getUnreadNotificationCount(User user);

    void deleteNotification(Long userId, Long notificationId);

    void deleteNotification(Long notificationId, User user);

    void sendApplicationStatusUpdate(User candidate, User recruiter, String jobTitle, String newStatus);

    void sendInterviewScheduled(User candidate, User recruiter, String jobTitle, LocalDateTime interviewTime);

    void sendJobPostedNotification(List<User> candidates, String jobTitle, String companyName);

    void sendWelcomeNotification(User user);

    void createNotification(User user, String string, String string2, String string3, Long id, String string4);
}
