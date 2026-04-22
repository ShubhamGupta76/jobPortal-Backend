package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.dto.NotificationDto;
import com.job_Portal_Backend.job_portal_backend.entity.Notification;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.NotificationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNotificationToUser(Long userId, String type, String title, String message, String data) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        sendNotificationToUser(user, title, message, type);
    }

    @Override
    public void sendNotificationToUser(User user, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        Notification savedNotification = notificationRepository.save(notification);

        // Send real-time notification via WebSocket
        NotificationDto dto = convertToDto(savedNotification);
        messagingTemplate.convertAndSendToUser(
                user.getEmail(),
                "/queue/notifications",
                dto);
    }

    @Override
    public void sendNotificationToUsers(List<Long> userIds, String type, String title, String message, String data) {
        List<User> users = userRepository.findAllById(userIds);
        sendNotificationToUsers(users, title, message, type);
    }

    @Override
    public void sendBulkNotification(String title, String message, String type) {
        List<User> allUsers = userRepository.findAllNotDeleted();
        sendNotificationToUsers(allUsers, title, message, type);
    }

    @Override
    public List<NotificationDto> getUserNotifications(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<NotificationDto> notifications = getUserNotifications(user, false);
        int from = Math.min(page * size, notifications.size());
        int to = Math.min(from + size, notifications.size());
        return notifications.subList(from, to);
    }

    @Override
    public List<NotificationDto> getUserNotifications(User user, boolean unreadOnly) {
        List<Notification> notifications = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        if (unreadOnly) {
            notifications = notifications.stream()
                    .filter(notification -> !notification.getRead())
                    .collect(Collectors.toList());
        }

        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationDto markNotificationAsRead(Long userId, Long notificationId) {
        Notification notification = findNotificationByIdForUser(userId, notificationId);
        if (notification != null && !notification.getRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
        return convertToDto(notification);
    }

    @Override
    public NotificationDto markNotificationAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return convertToDto(notification);
    }

    @Override
    public void markAllNotificationsAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        markAllNotificationsAsRead(user);
    }

    @Override
    public void markAllNotificationsAsRead(User user) {
        List<Notification> unreadNotifications = notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(notification -> !notification.getRead())
                .collect(Collectors.toList());
        for (Notification notification : unreadNotifications) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    @Override
    public long getUnreadNotificationCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public long getUnreadNotificationCount(User user) {
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Override
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = findNotificationByIdForUser(userId, notificationId);
        if (notification != null) {
            notificationRepository.delete(notification);
        }
    }

    @Override
    public void deleteNotification(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .orElse(null);
        if (notification != null) {
            notificationRepository.delete(notification);
        }
    }

    @Override
    public void sendApplicationStatusUpdate(User candidate, User recruiter, String jobTitle, String newStatus) {
        // Notify candidate
        String candidateTitle = "Application Status Update";
        String candidateMessage = String.format("Your application for '%s' has been %s", jobTitle,
                newStatus.toLowerCase());
        sendNotificationToUser(candidate, candidateTitle, candidateMessage, "APPLICATION_UPDATE");

        // Notify recruiter
        String recruiterTitle = "Application Status Changed";
        String recruiterMessage = String.format("Application from %s %s for '%s' has been %s",
                candidate.getFirstName(), candidate.getLastName(), jobTitle, newStatus.toLowerCase());
        sendNotificationToUser(recruiter, recruiterTitle, recruiterMessage, "APPLICATION_UPDATE");
    }

    @Override
    public void sendInterviewScheduled(User candidate, User recruiter, String jobTitle, LocalDateTime interviewTime) {
        // Notify candidate
        String candidateTitle = "Interview Scheduled";
        String candidateMessage = String.format("You have an interview scheduled for '%s' on %s",
                jobTitle, interviewTime.toString());
        sendNotificationToUser(candidate, candidateTitle, candidateMessage, "INTERVIEW_SCHEDULED");

        // Notify recruiter
        String recruiterTitle = "Interview Scheduled";
        String recruiterMessage = String.format("Interview scheduled with %s %s for '%s' on %s",
                candidate.getFirstName(), candidate.getLastName(), jobTitle, interviewTime.toString());
        sendNotificationToUser(recruiter, recruiterTitle, recruiterMessage, "INTERVIEW_SCHEDULED");
    }

    @Override
    public void sendJobPostedNotification(List<User> candidates, String jobTitle, String companyName) {
        String title = "New Job Opportunity";
        String message = String.format("New job posted: '%s' at %s", jobTitle, companyName);

        for (User candidate : candidates) {
            sendNotificationToUser(candidate, title, message, "NEW_JOB");
        }
    }

    @Override
    public void sendWelcomeNotification(User user) {
        String title = "Welcome to JobPortal!";
        String message = "Thank you for joining our platform. Start exploring job opportunities or post your first job.";
        sendNotificationToUser(user, title, message, "WELCOME");
    }

    @Override
    public void createNotification(User user, String title, String message, String type, Long relatedId, String data) {
        sendNotificationToUser(user, title, message, type);
    }

    private Notification findNotificationByIdForUser(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }
        return notification;
    }

    private NotificationDto convertToDto(Notification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .userId(notification.getUser().getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }

    @Override
    public void sendNotificationToUsers(List<User> users, String title, String message, String type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendNotificationToUsers'");
    }
}