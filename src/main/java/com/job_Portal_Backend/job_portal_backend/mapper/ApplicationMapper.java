package com.job_Portal_Backend.job_portal_backend.mapper;

import com.job_Portal_Backend.job_portal_backend.applications.dto.ApplicationDto;
import com.job_Portal_Backend.job_portal_backend.entity.Application;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationDto toDto(Application application) {
        ApplicationDto dto = new ApplicationDto();
        dto.setId(application.getId());
        dto.setUserId(application.getUser().getId());
        dto.setUserName((application.getUser().getFirstName() + " " + application.getUser().getLastName()).trim());
        dto.setJobId(application.getJob().getId());
        dto.setJobTitle(application.getJob().getTitle());
        dto.setStatus(application.getStatus().name());
        dto.setAssessmentId(application.getAssignedAssessment() != null ? application.getAssignedAssessment().getId() : null);
        dto.setAssessmentTitle(application.getAssignedAssessment() != null ? application.getAssignedAssessment().getTitle() : null);
        dto.setResumePath(application.getResumePath());
        dto.setResumeDownloadUrl("/api/v1/files/resume/" + application.getUser().getId());
        dto.setCoverLetter(application.getCoverLetter());
        dto.setCreatedAt(application.getCreatedAt());
        dto.setUpdatedAt(application.getUpdatedAt());
        return dto;
    }
}
