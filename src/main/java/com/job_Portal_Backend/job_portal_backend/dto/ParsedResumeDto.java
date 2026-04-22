package com.job_Portal_Backend.job_portal_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedResumeDto {
    private String fullName;
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    private String summary;
    private List<String> skills;
    private List<ExperienceDto> experience;
    private List<EducationDto> education;
    private List<String> certifications;
    private List<String> languages;
    private Map<String, Object> metadata;
    private double qualityScore;
    private List<String> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExperienceDto {
        private String jobTitle;
        private String company;
        private String location;
        private String startDate;
        private String endDate;
        private String description;
        private List<String> achievements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EducationDto {
        private String degree;
        private String institution;
        private String location;
        private String graduationDate;
        private String gpa;
        private List<String> honors;
    }
}