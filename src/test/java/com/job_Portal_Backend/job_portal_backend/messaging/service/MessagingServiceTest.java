package com.job_Portal_Backend.job_portal_backend.messaging.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.messaging.entity.Conversation;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.ConversationParticipantRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.ConversationRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageReadRepository;
import com.job_Portal_Backend.job_portal_backend.messaging.repository.MessageRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MessagingServiceTest {

    private final ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private final ConversationParticipantRepository participantRepository = mock(ConversationParticipantRepository.class);
    private final MessageRepository messageRepository = mock(MessageRepository.class);
    private final MessageReadRepository messageReadRepository = mock(MessageReadRepository.class);
    private final MessagingService service = new MessagingService(
            mock(ApplicationRepository.class), conversationRepository, participantRepository,
            messageRepository, messageReadRepository, mock(NotificationService.class), mock(org.springframework.messaging.simp.SimpMessagingTemplate.class));

    @Test
    void rejectsConversationAccessForNonParticipant() {
        Conversation conversation = new Conversation();
        conversation.setId(5L);
        when(conversationRepository.findById(5L)).thenReturn(Optional.of(conversation));
        when(participantRepository.findByConversationIdAndUserId(5L, 99L)).thenReturn(Optional.empty());

        User stranger = new User();
        stranger.setId(99L);

        assertThrows(RuntimeException.class, () -> service.getMessages(5L, stranger));
        verifyNoInteractions(messageRepository);
    }
}