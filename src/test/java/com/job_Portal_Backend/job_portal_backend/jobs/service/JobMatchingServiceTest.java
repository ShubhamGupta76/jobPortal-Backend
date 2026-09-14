package com.job_Portal_Backend.job_portal_backend.jobs.service;

import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobMatchingServiceTest {

    private final JobMatchingService service = new JobMatchingService();

    @Test
    void scoresSkillsExperienceLocationPreferencesAndProfileDeterministically() {
        User candidate = new User();
        candidate.setSkills("Java, Spring, PostgreSQL");
        candidate.setExperienceLevel("MID");
        candidate.setLocation("Bengaluru");
        candidate.setPreferredJobType("FULL_TIME");
        candidate.setPreferredWorkplaceType("HYBRID");
        candidate.setResumePath("resume.pdf");
        candidate.setHeadline("Backend engineer");

        Job job = new Job();
        job.setSkills("Java, Spring, React");
        job.setExperienceLevel("MID");
        job.setLocation("Bengaluru");
        job.setJobType("FULL_TIME");
        job.setWorkplaceType("HYBRID");

        JobMatchingService.MatchResult result = service.score(job, candidate);

        assertEquals(2, result.matchingSkills().size());
        assertEquals(List.of("REACT"), result.missingSkills());
        assertEquals(33, result.skillsScore());
        assertEquals(20, result.experienceScore());
        assertEquals(10, result.locationScore());
        assertEquals(15, result.preferenceScore());
        assertEquals(5, result.profileScore());
        assertEquals(83, result.score());
        assertTrue(result.reasons().stream().anyMatch(reason -> reason.contains("2 required skills")));
    }
}