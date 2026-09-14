package com.job_Portal_Backend.job_portal_backend.messaging.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.ConversationResponse;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.CreateConversationRequest;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.CreateMessageRequest;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.MessageResponse;
import com.job_Portal_Backend.job_portal_backend.messaging.service.MessagingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
@PreAuthorize("isAuthenticated()")
public class MessagingController {

    private final MessagingService messagingService;

    public MessagingController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Conversations retrieved successfully", messagingService.getConversations(currentUser(authentication))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConversationResponse>> createConversation(
            @Valid @RequestBody CreateConversationRequest request, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Conversation ready", messagingService.createOrGet(request, currentUser(authentication))));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Messages retrieved successfully", messagingService.getMessages(id, currentUser(authentication))));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable Long id, @Valid @RequestBody CreateMessageRequest request, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Message sent successfully", messagingService.sendMessage(id, request, currentUser(authentication))));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id, Authentication authentication) {
        messagingService.markRead(id, currentUser(authentication));
        return ResponseEntity.ok(new ApiResponse<>(true, "Conversation marked as read", null));
    }

    @MessageMapping("/conversations/{conversationId}/typing")
    public void typing(@DestinationVariable Long conversationId, @Payload com.job_Portal_Backend.job_portal_backend.messaging.dto.TypingRequest request, Authentication authentication) {
        messagingService.publishTyping(conversationId, request.isTyping(), currentUser(authentication));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Authenticated user not found");
        }
        return user;
    }
}