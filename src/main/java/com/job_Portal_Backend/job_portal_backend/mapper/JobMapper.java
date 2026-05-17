package com.job_Portal_Backend.job_portal_backend.mapper;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobDto toDto(Job job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setDescription(job.getDescription());
        dto.setDepartment(job.getDepartment());
        dto.setLocation(job.getLocation());
        dto.setWorkplaceType(job.getWorkplaceType());
        dto.setJobType(job.getJobType());
        dto.setExperienceLevel(job.getExperienceLevel());
        dto.setMinSalary(job.getMinSalary());
        dto.setMaxSalary(job.getMaxSalary());
        dto.setSkills(job.getSkills());
        dto.setStatus(job.getStatus());
        dto.setCompanyId(job.getCompany() != null ? job.getCompany().getId() : null);
        dto.setCompanyName(job.getCompany() != null ? job.getCompany().getName() : null);
        dto.setRecruiterId(job.getRecruiter() != null ? job.getRecruiter().getId() : null);
        dto.setRecruiterName(job.getRecruiter() != null
                ? (job.getRecruiter().getFirstName() + " " + job.getRecruiter().getLastName()).trim()
                : null);
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }
}
