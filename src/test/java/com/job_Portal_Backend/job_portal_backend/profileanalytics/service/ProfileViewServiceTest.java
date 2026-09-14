package com.job_Portal_Backend.job_portal_backend.profileanalytics.service;

import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ProfileViewEventRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfileViewServiceTest {

    private final ProfileViewEventRepository profileViewEventRepository = mock(ProfileViewEventRepository.class);
    private final ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    private final ProfileViewService service = new ProfileViewService(profileViewEventRepository, applicationRepository);

    @Test
    void ignoresSelfViews() {
        User candidate = user(1L, "USER");
        service.recordViewIfAuthorized(candidate, candidate, "APPLICATION");
        verifyNoInteractions(profileViewEventRepository);
    }

    @Test
    void ignoresNonRecruiterViewers() {
        User candidate = user(1L, "USER");
        User otherCandidate = user(2L, "USER");
        service.recordViewIfAuthorized(candidate, otherCandidate, "APPLICATION");
        verifyNoInteractions(profileViewEventRepository);
    }

    @Test
    void ignoresRecruiterWithNoHiringRelationship() {
        User candidate = user(1L, "USER");
        User recruiter = user(2L, "RECRUITER");
        when(applicationRepository.existsByUserIdAndJobRecruiterIdAndNotDeleted(1L, 2L)).thenReturn(false);

        service.recordViewIfAuthorized(candidate, recruiter, "APPLICATION");

        verifyNoInteractions(profileViewEventRepository);
    }

    @Test
    void recordsViewForRecruiterWithRealApplication() {
        User candidate = user(1L, "USER");
        User recruiter = user(2L, "RECRUITER");
        when(applicationRepository.existsByUserIdAndJobRecruiterIdAndNotDeleted(1L, 2L)).thenReturn(true);

        service.recordViewIfAuthorized(candidate, recruiter, "APPLICATION");

        verify(profileViewEventRepository).save(any());
    }

    private User user(Long id, String roleName) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        Role role = new Role();
        role.setName(roleName);
        user.setRoles(Set.of(role));
        return user;
    }
}
