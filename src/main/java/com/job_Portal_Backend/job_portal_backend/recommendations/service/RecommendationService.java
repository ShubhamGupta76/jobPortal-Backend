package com.job_Portal_Backend.job_portal_backend.recommendations.service;

import com.job_Portal_Backend.job_portal_backend.entity.Bookmark;
import com.job_Portal_Backend.job_portal_backend.entity.DismissedJob;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.recommendations.dto.RecommendedJobDto;
import com.job_Portal_Backend.job_portal_backend.repository.ApplicationRepository;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.DismissedJobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ranks jobs for a candidate's "Recommended for you" surfaces.
 *
 * <p>JobMatchingService remains the single source of truth for the base 0-100 candidate/job
 * match score (skills + experience + location + preferences + profile completeness) — this
 * service never recomputes or duplicates that scoring. It only adds small, bounded, deterministic
 * boosts on top of that base score using data that already exists elsewhere in the app:
 *
 * <pre>
 * finalScore = min(100, baseMatchScore
 *                        + (recentlyPosted        ? FRESHNESS_BOOST : 0)
 *                        + (companyVerified       ? VERIFIED_COMPANY_BOOST : 0)
 *                        + (similarToSavedJobs    ? SAVED_JOB_SIMILARITY_BOOST : 0))
 * </pre>
 *
 * Each boost is small relative to the 100-point base score so the base match always dominates
 * the ranking; the boosts only break ties and nudge otherwise-similar jobs, matching the
 * requirement that this stay explainable rather than an opaque weighting scheme.
 */
@Service
public class RecommendationService {

    // Each boost is intentionally small (5 points) relative to the 100-point base match score,
    // so contextual signals can only nudge ranking/break ties, never dominate the base match.
    static final int FRESHNESS_BOOST = 5;
    static final int VERIFIED_COMPANY_BOOST = 5;
    static final int SAVED_JOB_SIMILARITY_BOOST = 5;

    static final int FRESHNESS_WINDOW_DAYS = 7;
    static final int SIMILARITY_SKILL_OVERLAP_THRESHOLD = 2;

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final ApplicationRepository applicationRepository;
    private final BookmarkRepository bookmarkRepository;
    private final DismissedJobRepository dismissedJobRepository;

    public RecommendationService(JobRepository jobRepository,
            JobMapper jobMapper,
            ApplicationRepository applicationRepository,
            BookmarkRepository bookmarkRepository,
            DismissedJobRepository dismissedJobRepository) {
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
        this.applicationRepository = applicationRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.dismissedJobRepository = dismissedJobRepository;
    }

    @Transactional(readOnly = true)
    public Page<RecommendedJobDto> getRecommendations(User candidate, int page, int size, Integer minScore) {
        List<RecommendedJobDto> ranked = rankAllEligibleJobs(candidate);

        List<RecommendedJobDto> filtered = minScore == null
                ? ranked
                : ranked.stream().filter(item -> item.getFinalScore() >= minScore).toList();

        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        return new PageImpl<>(filtered.subList(from, to), PageRequest.of(page, size), total);
    }

    /** Used by the candidate dashboard's existing "Recommended for you" preview so both surfaces share one ranking. */
    @Transactional(readOnly = true)
    public List<RecommendedJobDto> getTopRecommendations(User candidate, int limit) {
        return rankAllEligibleJobs(candidate).stream().limit(limit).toList();
    }

    @Transactional
    public void dismiss(User candidate, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (dismissedJobRepository.existsByUserIdAndJobId(candidate.getId(), jobId)) {
            return;
        }
        DismissedJob dismissed = new DismissedJob();
        dismissed.setUser(candidate);
        dismissed.setJob(job);
        dismissedJobRepository.save(dismissed);
    }

    @Transactional
    public void undoDismiss(User candidate, Long jobId) {
        dismissedJobRepository.findByUserIdAndJobId(candidate.getId(), jobId)
                .ifPresent(dismissedJobRepository::delete);
    }

    // --- ranking pipeline ------------------------------------------------

    private List<RecommendedJobDto> rankAllEligibleJobs(User candidate) {
        // Step 3 exclusions: reuse existing data rather than a second "eligibility" query.
        // findJobsWithFilters already restricts to status=ACTIVE (or null) and isDeleted=false,
        // which is the same visibility rule the public job search already enforces.
        Set<Long> excludedJobIds = new HashSet<>();
        applicationRepository.findByUserIdAndNotDeleted(candidate.getId())
                .forEach(application -> excludedJobIds.add(application.getJob().getId()));

        List<Bookmark> savedJobs = bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(candidate.getId());
        savedJobs.forEach(bookmark -> excludedJobIds.add(bookmark.getJob().getId()));

        dismissedJobRepository.findByUserIdOrderByCreatedAtDesc(candidate.getId())
                .forEach(dismissed -> excludedJobIds.add(dismissed.getJob().getId()));

        Set<String> savedJobSkills = savedJobs.stream()
                .map(bookmark -> bookmark.getJob().getSkills())
                .flatMap(this::splitSkills)
                .collect(Collectors.toSet());

        LocalDateTime freshnessCutoff = LocalDateTime.now().minusDays(FRESHNESS_WINDOW_DAYS);

        List<RecommendedJobDto> scored = jobRepository.findJobsWithFilters(null, null, null, null, null, null).stream()
                .filter(job -> !excludedJobIds.contains(job.getId()))
                .filter(job -> job.getCompany() == null || !Boolean.TRUE.equals(job.getCompany().getIsDeleted()))
                .map(job -> scoreJob(job, candidate, savedJobSkills, freshnessCutoff))
                .collect(Collectors.toCollection(ArrayList::new));

        scored.sort(Comparator
                .comparingInt(RecommendedJobDto::getFinalScore).reversed()
                .thenComparing((RecommendedJobDto item) -> item.getJob().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(item -> item.getJob().getId()));

        return scored;
    }

    private RecommendedJobDto scoreJob(Job job, User candidate, Set<String> savedJobSkills, LocalDateTime freshnessCutoff) {
        JobDto dto = jobMapper.toDto(job, candidate);
        int baseScore = dto.getMatchScore() != null ? dto.getMatchScore() : 0;

        List<String> reasonCodes = new ArrayList<>();
        if (dto.getMatchingSkills() != null && !dto.getMatchingSkills().isEmpty()) {
            reasonCodes.add(RecommendationReason.MATCH_SKILLS);
        }
        if (dto.getExperienceScore() != null && dto.getExperienceScore() > 0) {
            reasonCodes.add(RecommendationReason.MATCH_EXPERIENCE);
        }
        if (dto.getLocationScore() != null && dto.getLocationScore() >= 10) {
            reasonCodes.add(RecommendationReason.MATCH_LOCATION);
        }
        if (dto.getPreferenceScore() != null && dto.getPreferenceScore() > 0) {
            reasonCodes.add(RecommendationReason.MATCH_EMPLOYMENT_TYPE);
        }

        int boost = 0;
        if (job.getCreatedAt() != null && job.getCreatedAt().isAfter(freshnessCutoff)) {
            boost += FRESHNESS_BOOST;
            reasonCodes.add(RecommendationReason.RECENTLY_POSTED);
        }
        if ("VERIFIED".equalsIgnoreCase(dto.getCompanyVerificationStatus())) {
            boost += VERIFIED_COMPANY_BOOST;
            reasonCodes.add(RecommendationReason.VERIFIED_COMPANY);
        }
        if (!savedJobSkills.isEmpty() && overlapCount(splitSkills(job.getSkills()), savedJobSkills) >= SIMILARITY_SKILL_OVERLAP_THRESHOLD) {
            boost += SAVED_JOB_SIMILARITY_BOOST;
            reasonCodes.add(RecommendationReason.SIMILAR_TO_SAVED_JOB);
        }

        RecommendedJobDto result = new RecommendedJobDto();
        result.setJob(dto);
        result.setFinalScore(Math.min(100, baseScore + boost));
        result.setReasonCodes(reasonCodes);
        return result;
    }

    private long overlapCount(java.util.stream.Stream<String> jobSkills, Set<String> savedJobSkills) {
        return jobSkills.filter(savedJobSkills::contains).count();
    }

    private java.util.stream.Stream<String> splitSkills(String skills) {
        if (skills == null || skills.isBlank()) {
            return java.util.stream.Stream.empty();
        }
        return Arrays.stream(skills.split(","))
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank());
    }
}
