package com.job_Portal_Backend.job_portal_backend.notificationpreferences.service;

import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference;
import com.job_Portal_Backend.job_portal_backend.entity.NotificationPreference.Category;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.notificationpreferences.dto.NotificationPreferenceDto;
import com.job_Portal_Backend.job_portal_backend.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationPreferenceServiceTest {

    private final NotificationPreferenceRepository repository = mock(NotificationPreferenceRepository.class);
    private final NotificationPreferenceResolver resolver = new NotificationPreferenceResolver(repository);
    private final NotificationPreferenceService service = new NotificationPreferenceService(repository, resolver);

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    @Test
    void defaultPreferencesPreserveExistingBehavior() {
        User user = user(1L);
        when(repository.findByUserId(1L)).thenReturn(List.of());
        when(repository.findByUserIdAndCategory(eq(1L), any())).thenReturn(java.util.Optional.empty());

        List<NotificationPreferenceDto> preferences = service.getPreferences(user);

        assertEquals(Category.values().length, preferences.size());
        // Every category defaults to IN_APP on / EMAIL off, matching current unconditional
        // in-app delivery and the complete absence of any existing email-sending code path.
        assertTrue(preferences.stream().allMatch(NotificationPreferenceDto::isInAppEnabled));
        assertTrue(preferences.stream().noneMatch(NotificationPreferenceDto::isEmailEnabled));
    }

    @Test
    void userCanRetrieveOwnPreferencesCoveringAllCategories() {
        User user = user(1L);
        NotificationPreference existing = new NotificationPreference();
        existing.setId(10L);
        existing.setUser(user);
        existing.setCategory(Category.MESSAGES);
        existing.setInAppEnabled(true);
        existing.setEmailEnabled(true);
        when(repository.findByUserIdAndCategory(1L, Category.MESSAGES)).thenReturn(java.util.Optional.of(existing));
        when(repository.findByUserIdAndCategory(eq(1L), any())).thenAnswer(invocation -> {
            Category category = invocation.getArgument(1);
            return category == Category.MESSAGES ? java.util.Optional.of(existing) : java.util.Optional.empty();
        });

        List<NotificationPreferenceDto> preferences = service.getPreferences(user);

        NotificationPreferenceDto messages = preferences.stream()
                .filter(p -> p.getCategory().equals("MESSAGES")).findFirst().orElseThrow();
        assertTrue(messages.isEmailEnabled());
    }

    @Test
    void updatingAnExistingCategoryUpdatesTheSameRowRatherThanCreatingADuplicate() {
        User user = user(1L);
        NotificationPreference existing = new NotificationPreference();
        existing.setId(10L);
        existing.setUser(user);
        existing.setCategory(Category.MESSAGES);
        existing.setInAppEnabled(true);
        existing.setEmailEnabled(false);

        when(repository.findByUserId(1L)).thenReturn(List.of(existing));
        when(repository.findByUserIdAndCategory(eq(1L), any())).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updatePreferences(user, List.of(new NotificationPreferenceDto("MESSAGES", true, true)));

        verify(repository, times(1)).save(argThat(pref -> pref.getId() != null && pref.getId().equals(10L)));
    }

    @Test
    void updateAlwaysScopesToTheProvidedUserNeverAnotherUser() {
        User userOne = user(1L);
        when(repository.findByUserId(1L)).thenReturn(List.of());
        when(repository.findByUserIdAndCategory(eq(1L), any())).thenReturn(java.util.Optional.empty());
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updatePreferences(userOne, List.of(new NotificationPreferenceDto("MESSAGES", false, false)));

        verify(repository).save(argThat(pref -> pref.getUser().getId().equals(1L)));
    }

    @Test
    void invalidCategoryIsRejected() {
        User user = user(1L);
        when(repository.findByUserId(1L)).thenReturn(List.of());

        assertThrows(ResponseStatusException.class, () -> service.updatePreferences(user,
                List.of(new NotificationPreferenceDto("NOT_A_REAL_CATEGORY", true, true))));
    }

    @Test
    void omittedCategoriesRemainUntouched() {
        User user = user(1L);
        NotificationPreference existingMessages = new NotificationPreference();
        existingMessages.setId(10L);
        existingMessages.setUser(user);
        existingMessages.setCategory(Category.MESSAGES);
        existingMessages.setInAppEnabled(true);
        existingMessages.setEmailEnabled(true);

        when(repository.findByUserId(1L)).thenReturn(List.of(existingMessages));
        when(repository.findByUserIdAndCategory(eq(1L), any())).thenAnswer(invocation -> {
            Category category = invocation.getArgument(1);
            return category == Category.MESSAGES ? java.util.Optional.of(existingMessages) : java.util.Optional.empty();
        });
        when(repository.save(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Only updates JOB_ALERTS; MESSAGES (already ON/ON) must remain untouched.
        service.updatePreferences(user, List.of(new NotificationPreferenceDto("JOB_ALERTS", false, false)));

        verify(repository, never()).save(argThat(pref -> pref.getCategory() == Category.MESSAGES));
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
