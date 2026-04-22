package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.dto.ParsedResumeDto;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ResumeParsingService {

    ParsedResumeDto parseResume(MultipartFile resumeFile) throws IOException;

    ParsedResumeDto parseResumeFromText(String resumeText);

    List<String> extractSkills(String text);

    List<String> extractExperience(String text);

    List<String> extractEducation(String text);

    String extractContactInfo(String text);

    String extractSummary(String text);

    Map<String, Double> calculateSkillMatchScore(List<String> resumeSkills, List<String> jobSkills);

    double calculateOverallMatchScore(ParsedResumeDto parsedResume, String jobDescription, List<String> jobSkills);

    boolean validateResumeFormat(MultipartFile file);

    String extractTextFromFile(MultipartFile file) throws IOException;

    List<String> getSupportedFileTypes();

    Map<String, Object> analyzeResumeQuality(ParsedResumeDto parsedResume);
}