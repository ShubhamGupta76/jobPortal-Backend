package com.job_Portal_Backend.job_portal_backend.profileanalytics.service;

import com.job_Portal_Backend.job_portal_backend.entity.ProfileViewEvent;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.ProfileViewEventRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileViewService {

    private final ProfileViewEventRepository profileViewEventRepository;
    private final ApplicationRepository applicationRepository;

    public ProfileViewService(ProfileViewEventRepository profileViewEventRepository,
            ApplicationRepository applicationRepository) {
        this.profileViewEventRepository = profileViewEventRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * Records a profile view only when the viewer is a different, authenticated recruiter
     * who has a real hiring relationship with the candidate (an application to one of the
     * recruiter's jobs). Self-views and unrelated lookups are silently ignored rather than
     * inflating a candidate's analytics.
     */
    public void recordViewIfAuthorized(User viewedUser, User viewer, String source) {
        if (viewedUser == null || viewer == null || viewedUser.getId().equals(viewer.getId())) {
            return;
        }
        boolean isRecruiter = viewer.getRoles().stream().anyMatch(role -> "RECRUITER".equalsIgnoreCase(role.getName()));
        if (!isRecruiter) {
            return;
        }
        boolean hasRelationship = applicationRepository
                .existsByUserIdAndJobRecruiterIdAndNotDeleted(viewedUser.getId(), viewer.getId());
        if (!hasRelationship) {
            return;
        }

        ProfileViewEvent event = new ProfileViewEvent();
        event.setViewedUser(viewedUser);
        event.setViewedBy(viewer);
        event.setSource(source);
        profileViewEventRepository.save(event);
    }
}
