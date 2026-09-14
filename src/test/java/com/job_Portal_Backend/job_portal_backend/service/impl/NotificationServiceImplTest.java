package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.entity.Notification;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.notificationpreferences.service.NotificationPreferenceResolver;
import com.job_Portal_Backend.job_portal_backend.notificationpreferences.service.NotificationPreferenceResolver.Effective;
import com.job_Portal_Backend.job_portal_backend.repository.NotificationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Verifies that notification preferences are enforced centrally inside the one method every
 * NotificationService call site funnels through, without touching any of those call sites.
 */
class NotificationServiceImplTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final NotificationPreferenceResolver preferenceResolver = mock(NotificationPreferenceResolver.class);
    private final EmailService emailService = mock(EmailService.class);

    private final NotificationServiceImpl service = new NotificationServiceImpl();

    NotificationServiceImplTest() {
        ReflectionTestUtils.setField(service, "notificationRepository", notificationRepository);
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "messagingTemplate", messagingTemplate);
        ReflectionTestUtils.setField(service, "notificationPreferenceResolver", preferenceResolver);
        ReflectionTestUtils.setField(service, "emailService", emailService);
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail("candidate@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        return user;
    }

    @Test
    void inAppEnabledPreservesExistingBehaviorExactly() {
        User user = user();
        when(preferenceResolver.resolve(eq(1L), any())).thenReturn(new Effective(true, false));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendNotificationToUser(user, "Title", "Message", "APPLICATION", null);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(eq("candidate@example.com"), eq("/queue/notifications"), any());
        verifyNoInteractions(emailService);
    }

    @Test
    void inAppDisabledSuppressesPersistenceAndWebSocketPush() {
        User user = user();
        when(preferenceResolver.resolve(eq(1L), any())).thenReturn(new Effective(false, false));

        service.sendNotificationToUser(user, "Title", "Message", "APPLICATION", null);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void emailEnabledSendsThroughExistingEmailService() {
        User user = user();
        when(preferenceResolver.resolve(eq(1L), any())).thenReturn(new Effective(true, true));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendNotificationToUser(user, "Title", "Message body", "JOB_ALERT", null);

        verify(emailService).sendBulkEmail(eq(new String[] { "candidate@example.com" }), eq("Title"), eq("Message body"));
    }

    @Test
    void emailDisabledByDefaultNeverSendsEmail() {
        User user = user();
        when(preferenceResolver.resolve(eq(1L), any())).thenReturn(new Effective(true, false));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendNotificationToUser(user, "Title", "Message", "JOB_ALERT", null);

        verifyNoInteractions(emailService);
    }

    @Test
    void emailFailureNeverBreaksTheNotificationFlow() {
        User user = user();
        when(preferenceResolver.resolve(eq(1L), any())).thenReturn(new Effective(true, true));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("SMTP down")).when(emailService).sendBulkEmail(any(), any(), any());

        assertDoesNotThrow(() -> service.sendNotificationToUser(user, "Title", "Message", "JOB_ALERT", null));
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void oneEventProducesExactlyOneNotificationAndOneEmailAttempt() {
        User user = user();
        when(preferenceResolver.resolve(eq(1L), any())).thenReturn(new Effective(true, true));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.sendNotificationToUser(user, "New job matches your search", "Job matched", "JOB_ALERT", null);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
        verify(emailService, times(1)).sendBulkEmail(any(), any(), any());
    }
}
