package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDto {
    private Long id;
    private String title;
    private String description;
    private String department;
    private String location;
    private String workplaceType;
    private String jobType;
    private String experienceLevel;
    private Double minSalary;
    private Double maxSalary;
    private String skills;
    private String status;
    private Long applicationCount;
    private Long companyId;
    private String companyName;
    private String companyVerificationStatus;
    private Long recruiterId;
    private String recruiterName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer matchScore;
    private List<String> matchingSkills;
    private List<String> missingSkills;
    private String experienceMatch;
    private String locationMatch;
    private List<String> matchReasons;
    private Integer skillsScore;
    private Integer experienceScore;
    private Integer locationScore;
    private Integer preferenceScore;
    private Integer profileScore;
}
