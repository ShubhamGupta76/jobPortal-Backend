package com.job_Portal_Backend.job_portal_backend.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.dto.NotificationDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            Authentication authentication) {
        User user = currentUser(authentication);
        List<NotificationDto> notifications = notificationService.getUserNotifications(user, false);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(
            Authentication authentication) {
        User user = currentUser(authentication);
        List<NotificationDto> notifications = notificationService.getUserNotifications(user, true);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread notifications retrieved successfully", notifications));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        User user = currentUser(authentication);
        long count = notificationService.getUnreadNotificationCount(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread count retrieved successfully", count));
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long notificationId,
            Authentication authentication) {
        User user = currentUser(authentication);
        NotificationDto notification = notificationService.markNotificationAsRead(notificationId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", notification));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Authentication authentication) {
        User user = currentUser(authentication);
        notificationService.markAllNotificationsAsRead(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read", "Success"));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Authenticated user not found");
        }
        return user;
    }
}
