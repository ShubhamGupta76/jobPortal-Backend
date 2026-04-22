package com.job_Portal_Backend.job_portal_backend.controller;

import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.dto.NotificationDto;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    public NotificationController(NotificationService notificationService, JwtService jwtService) {
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getNotifications(
            @RequestHeader("Authorization") String token) {
        User user = jwtService.getCurrentUser();
        List<NotificationDto> notifications = notificationService.getUserNotifications(user, false);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(
            @RequestHeader("Authorization") String token) {
        User user = jwtService.getCurrentUser();
        List<NotificationDto> notifications = notificationService.getUserNotifications(user, true);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread notifications retrieved successfully", notifications));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@RequestHeader("Authorization") String token) {
        User user = jwtService.getCurrentUser();
        long count = notificationService.getUnreadNotificationCount(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Unread count retrieved successfully", count));
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationDto>> markAsRead(@PathVariable Long notificationId,
            @RequestHeader("Authorization") String token) {
        User user = jwtService.getCurrentUser();
        NotificationDto notification = notificationService.markNotificationAsRead(notificationId, user);
        return ResponseEntity.ok(new ApiResponse<>(true, "Notification marked as read", notification));
    }

    @PutMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(@RequestHeader("Authorization") String token) {
        User user = jwtService.getCurrentUser();
        notificationService.markAllNotificationsAsRead(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "All notifications marked as read", "Success"));
    }

}
