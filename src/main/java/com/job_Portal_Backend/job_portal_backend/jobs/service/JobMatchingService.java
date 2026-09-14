package com.job_Portal_Backend.job_portal_backend.jobs.service;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JobMatchingService {

    public MatchResult score(Job job, User candidate) {
        Set<String> requiredSkills = normalizeSkills(job.getSkills());
        Set<String> candidateSkills = normalizeSkills(candidate.getSkills());
        List<String> matchingSkills = requiredSkills.stream().filter(candidateSkills::contains).toList();
        List<String> missingSkills = requiredSkills.stream().filter(skill -> !candidateSkills.contains(skill)).toList();
        int skillsScore = requiredSkills.isEmpty() ? 0 : Math.round((matchingSkills.size() * 50f) / requiredSkills.size());
        int experienceScore = scoreExperience(candidate.getExperienceLevel(), job.getExperienceLevel());
        int locationScore = scoreLocation(candidate, job);
        int preferenceScore = scorePreferences(candidate, job);
        int profileScore = scoreProfile(candidate);
        int totalScore = Math.min(100, skillsScore + experienceScore + locationScore + preferenceScore + profileScore);

        List<String> reasons = new ArrayList<>();
        if (!matchingSkills.isEmpty()) reasons.add("You match " + matchingSkills.size() + " required skill" + (matchingSkills.size() == 1 ? "" : "s"));
        if (experienceScore == 20) reasons.add("Your experience level matches the role");
        else if (experienceScore > 0) reasons.add("Your experience is close to the requested level");
        if (locationScore == 10) reasons.add("The location or work mode matches your preference");
        if (preferenceScore > 0) reasons.add("The job fits your work preferences");
        if (profileScore == 5) reasons.add("Your profile is ready for recruiter review");
        if (reasons.isEmpty()) reasons.add("Complete your profile to improve this match");

        return new MatchResult(totalScore, matchingSkills, missingSkills,
                experienceScore == 20 ? "Strong match" : experienceScore > 0 ? "Close match" : "Not provided",
                locationScore == 10 ? "Match" : locationScore > 0 ? "Partially matched" : "Different location",
                reasons, skillsScore, experienceScore, locationScore, preferenceScore, profileScore);
    }

    private int scoreExperience(String candidateLevel, String jobLevel) {
        if (isBlank(candidateLevel) || isBlank(jobLevel)) return 0;
        List<String> levels = List.of("ENTRY", "MID", "SENIOR", "EXECUTIVE");
        int candidateIndex = levels.indexOf(normalize(candidateLevel));
        int jobIndex = levels.indexOf(normalize(jobLevel));
        if (candidateIndex < 0 || jobIndex < 0) return normalize(candidateLevel).equals(normalize(jobLevel)) ? 20 : 0;
        int distance = Math.abs(candidateIndex - jobIndex);
        return distance == 0 ? 20 : distance == 1 ? 12 : 0;
    }

    private int scoreLocation(User candidate, Job job) {
        if (isBlank(candidate.getLocation()) || isBlank(job.getLocation())) return 5;
        return containsIgnoreCase(candidate.getLocation(), job.getLocation())
                || containsIgnoreCase(job.getLocation(), candidate.getLocation()) ? 10 : 0;
    }

    private int scorePreferences(User candidate, Job job) {
        int score = 0;
        if (equalsIgnoreCase(candidate.getPreferredJobType(), job.getJobType())) score += 7;
        if (equalsIgnoreCase(candidate.getPreferredWorkplaceType(), job.getWorkplaceType())) score += 8;
        return score;
    }

    private int scoreProfile(User candidate) {
        int populatedFields = 0;
        if (!isBlank(candidate.getSkills())) populatedFields++;
        if (!isBlank(candidate.getExperienceLevel())) populatedFields++;
        if (!isBlank(candidate.getLocation())) populatedFields++;
        if (!isBlank(candidate.getResumePath())) populatedFields++;
        if (!isBlank(candidate.getHeadline()) || !isBlank(candidate.getBio())) populatedFields++;
        return Math.min(populatedFields, 5);
    }

    private Set<String> normalizeSkills(String skills) {
        if (isBlank(skills)) return Collections.emptySet();
        return Arrays.stream(skills.split(",")).map(this::normalize).filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean containsIgnoreCase(String first, String second) {
        return normalize(first).contains(normalize(second));
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return !isBlank(first) && !isBlank(second) && normalize(first).equals(normalize(second));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MatchResult(int score, List<String> matchingSkills, List<String> missingSkills,
                              String experienceMatch, String locationMatch, List<String> reasons,
                              int skillsScore, int experienceScore, int locationScore,
                              int preferenceScore, int profileScore) {
    }
}