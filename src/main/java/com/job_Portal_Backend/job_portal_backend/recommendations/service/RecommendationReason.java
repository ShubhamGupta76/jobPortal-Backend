package com.job_Portal_Backend.job_portal_backend.recommendations.service;

/**
 * Deterministic reason codes explaining why a job was recommended. The frontend maps
 * these codes to user-facing labels; the backend never emits free-text explanations here
 * (JobMatchingService's own free-text reasons remain on JobDto.matchReasons, unchanged).
 */
public final class RecommendationReason {

    public static final String MATCH_SKILLS = "MATCH_SKILLS";
    public static final String MATCH_EXPERIENCE = "MATCH_EXPERIENCE";
    public static final String MATCH_LOCATION = "MATCH_LOCATION";
    public static final String MATCH_EMPLOYMENT_TYPE = "MATCH_EMPLOYMENT_TYPE";
    public static final String SIMILAR_TO_SAVED_JOB = "SIMILAR_TO_SAVED_JOB";
    public static final String VERIFIED_COMPANY = "VERIFIED_COMPANY";
    public static final String RECENTLY_POSTED = "RECENTLY_POSTED";

    private RecommendationReason() {
    }
}
