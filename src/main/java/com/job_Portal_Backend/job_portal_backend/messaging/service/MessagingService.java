package com.job_Portal_Backend.job_portal_backend.messaging.service;

import com.job_Portal_Backend.job_portal_backend.entity.Application;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.ConversationResponse;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.CreateConversationRequest;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.CreateMessageRequest;
import com.job_Portal_Backend.job_portal_backend.messaging.dto.MessageResponse;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.Conversation;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.ConversationParticipant;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.Message;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.MessageRead;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.ConversationParticipantRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.ConversationRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageReadRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MessagingService {

    private final ApplicationRepository applicationRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessagingService(ApplicationRepository applicationRepository,
                            ConversationRepository conversationRepository,
                            ConversationParticipantRepository participantRepository,
                            MessageRepository messageRepository,
                            MessageReadRepository messageReadRepository,
                            NotificationService notificationService,
                            SimpMessagingTemplate messagingTemplate) {
        this.applicationRepository = applicationRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.messageReadRepository = messageReadRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public ConversationResponse createOrGet(CreateConversationRequest request, User user) {
        Application application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        authorizeApplication(application, user);
        Conversation conversation = conversationRepository.findByApplicationId(application.getId()).orElse(null);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setApplication(application);
            conversation = conversationRepository.save(conversation);
            addParticipant(conversation, application.getUser(), "CANDIDATE");
            addParticipant(conversation, application.getJob().getRecruiter(), "RECRUITER");
        }
        authorizeConversation(conversation, user);
        return toConversationResponse(conversation, user);
    }

    public List<ConversationResponse> getConversations(User user) {
        return conversationRepository.findByParticipantsUserIdOrderByUpdatedAtDesc(user.getId()).stream()
                .map(conversation -> toConversationResponse(conversation, user))
                .toList();
    }

    public List<MessageResponse> getMessages(Long conversationId, User user) {
        Conversation conversation = authorizedConversation(conversationId, user);
        Long otherUserId = otherParticipant(conversation, user).getUser().getId();
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(message -> toMessageResponse(message, otherUserId))
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(Long conversationId, CreateMessageRequest request, User user) {
        Conversation conversation = authorizedConversation(conversationId, user);
        String type = request.getMessageType() == null ? "TEXT" : request.getMessageType().trim().toUpperCase();
        if (!List.of("TEXT", "SYSTEM").contains(type)) {
            throw new RuntimeException("Message type must be TEXT or SYSTEM");
        }
        String content = request.getContent().trim();
        if (content.isBlank() || content.length() > 4000) {
            throw new RuntimeException("Message content must be between 1 and 4000 characters");
        }
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(user);
        message.setContent(content);
        message.setMessageType(type);
        message = messageRepository.save(message);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        ConversationParticipant recipient = otherParticipant(conversation, user);
        MessageResponse response = toMessageResponse(message, recipient.getUser().getId());
        publishToParticipants(conversation, user, Map.of("event", "MESSAGE_CREATED", "message", response));
        notificationService.sendNotificationToUser(recipient.getUser(), "New message", user.getFirstName() + " sent you a message.", "NEW_MESSAGE", "{\"conversationId\":" + conversationId + "}");
        return response;
    }

    @Transactional
    public void markRead(Long conversationId, User user) {
        Conversation conversation = authorizedConversation(conversationId, user);
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        LocalDateTime readAt = LocalDateTime.now();
        for (Message message : messages) {
            if (!message.getSender().getId().equals(user.getId())
                    && messageReadRepository.findByMessageIdAndUserId(message.getId(), user.getId()).isEmpty()) {
                MessageRead read = new MessageRead();
                read.setMessage(message);
                read.setUser(user);
                read.setReadAt(readAt);
                messageReadRepository.save(read);
            }
        }
        participantRepository.findByConversationIdAndUserId(conversationId, user.getId()).ifPresent(participant -> {
            participant.setLastReadAt(readAt);
            participantRepository.save(participant);
        });
        publishToParticipants(conversation, user, Map.of("event", "MESSAGE_READ", "conversationId", conversationId, "readAt", readAt));
    }

    public void publishTyping(Long conversationId, boolean typing, User user) {
        Conversation conversation = authorizedConversation(conversationId, user);
        ConversationParticipant recipient = otherParticipant(conversation, user);
        Map<String, Object> event = new HashMap<>();
        event.put("event", typing ? "TYPING_STARTED" : "TYPING_STOPPED");
        event.put("conversationId", conversationId);
        event.put("userId", user.getId());
        messagingTemplate.convertAndSendToUser(recipient.getUser().getEmail(), "/queue/messages", event);
    }

    private void addParticipant(Conversation conversation, User user, String role) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversation(conversation);
        participant.setUser(user);
        participant.setRole(role);
        participantRepository.save(participant);
    }

    private Conversation authorizedConversation(Long conversationId, User user) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        authorizeConversation(conversation, user);
        return conversation;
    }

    private void authorizeApplication(Application application, User user) {
        boolean allowed = application.getUser().getId().equals(user.getId())
                || application.getJob().getRecruiter().getId().equals(user.getId());
        if (!allowed) throw new RuntimeException("Unauthorized to message this hiring relationship");
    }

    private void authorizeConversation(Conversation conversation, User user) {
        if (participantRepository.findByConversationIdAndUserId(conversation.getId(), user.getId()).isEmpty()) {
            throw new RuntimeException("Unauthorized to access this conversation");
        }
    }

    private ConversationParticipant otherParticipant(Conversation conversation, User user) {
        return conversation.getParticipants().stream()
                .filter(participant -> !participant.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Conversation recipient not found"));
    }

    private ConversationResponse toConversationResponse(Conversation conversation, User user) {
        ConversationParticipant other = otherParticipant(conversation, user);
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        Message last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        long unread = messages.stream()
                .filter(message -> !message.getSender().getId().equals(user.getId()))
                .filter(message -> messageReadRepository.findByMessageIdAndUserId(message.getId(), user.getId()).isEmpty())
                .count();
        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setApplicationId(conversation.getApplication().getId());
        response.setJobId(conversation.getApplication().getJob().getId());
        response.setJobTitle(conversation.getApplication().getJob().getTitle());
        response.setOtherUserId(other.getUser().getId());
        response.setOtherUserName((other.getUser().getFirstName() + " " + other.getUser().getLastName()).trim());
        response.setLastMessage(last == null ? null : last.getContent());
        response.setLastMessageAt(last == null ? null : last.getCreatedAt());
        response.setUnreadCount(unread);
        response.setUpdatedAt(conversation.getUpdatedAt());
        return response;
    }

    private MessageResponse toMessageResponse(Message message, Long readerId) {
        MessageRead read = messageReadRepository.findByMessageIdAndUserId(message.getId(), readerId).orElse(null);
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        response.setConversationId(message.getConversation().getId());
        response.setSenderId(message.getSender().getId());
        response.setSenderName((message.getSender().getFirstName() + " " + message.getSender().getLastName()).trim());
        response.setContent(message.getContent());
        response.setMessageType(message.getMessageType());
        response.setCreatedAt(message.getCreatedAt());
        response.setRead(read != null);
        response.setReadAt(read == null ? null : read.getReadAt());
        return response;
    }

    private void publishToParticipants(Conversation conversation, User actor, Map<String, Object> event) {
        conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(user -> !user.getId().equals(actor.getId()))
                .forEach(user -> messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/messages", event));
    }
}