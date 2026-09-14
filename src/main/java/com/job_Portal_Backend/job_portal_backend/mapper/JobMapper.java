package com.job_Portal_Backend.job_portal_backend.mapper;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.jobs.service.JobMatchingService;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationRepository;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    private final JobMatchingService jobMatchingService;
    private final CompanyVerificationRepository companyVerificationRepository;

    public JobMapper(JobMatchingService jobMatchingService, CompanyVerificationRepository companyVerificationRepository) {
        this.jobMatchingService = jobMatchingService;
        this.companyVerificationRepository = companyVerificationRepository;
    }

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
        dto.setCompanyVerificationStatus(job.getCompany() != null
                ? companyVerificationRepository.findByCompanyId(job.getCompany().getId())
                        .map(v -> v.getStatus().name())
                        .orElse("UNVERIFIED")
                : null);
        dto.setRecruiterId(job.getRecruiter() != null ? job.getRecruiter().getId() : null);
        dto.setRecruiterName(job.getRecruiter() != null
                ? (job.getRecruiter().getFirstName() + " " + job.getRecruiter().getLastName()).trim()
                : null);
        dto.setCreatedAt(job.getCreatedAt());
        dto.setUpdatedAt(job.getUpdatedAt());
        return dto;
    }

    public JobDto toDto(Job job, User candidate) {
        JobDto dto = toDto(job);
        JobMatchingService.MatchResult match = jobMatchingService.score(job, candidate);
        dto.setMatchScore(match.score());
        dto.setMatchingSkills(match.matchingSkills());
        dto.setMissingSkills(match.missingSkills());
        dto.setExperienceMatch(match.experienceMatch());
        dto.setLocationMatch(match.locationMatch());
        dto.setMatchReasons(match.reasons());
        dto.setSkillsScore(match.skillsScore());
        dto.setExperienceScore(match.experienceScore());
        dto.setLocationScore(match.locationScore());
        dto.setPreferenceScore(match.preferenceScore());
        dto.setProfileScore(match.profileScore());
        return dto;
    }
}
