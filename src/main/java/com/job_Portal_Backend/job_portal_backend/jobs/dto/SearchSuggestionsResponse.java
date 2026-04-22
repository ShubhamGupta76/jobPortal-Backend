package com.job_Portal_Backend.job_portal_backend.jobs.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchSuggestionsResponse {
    private List<String> suggestions;
}
