package com.job_Portal_Backend.job_portal_backend.savedsearch.controller;

import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.savedsearch.dto.SavedSearchRequest;
import com.job_Portal_Backend.job_portal_backend.savedsearch.dto.SavedSearchResponse;
import com.job_Portal_Backend.job_portal_backend.savedsearch.service.SavedSearchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/saved-searches")
@PreAuthorize("hasRole('USER')")
public class SavedSearchController {

    private final SavedSearchService savedSearchService;

    public SavedSearchController(SavedSearchService savedSearchService) {
        this.savedSearchService = savedSearchService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedSearchResponse>>> getSavedSearches(Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved searches retrieved successfully", savedSearchService.getForUser(user)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavedSearchResponse>> createSavedSearch(
            @Valid @RequestBody SavedSearchRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved search created successfully", savedSearchService.create(request, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavedSearchResponse>> updateSavedSearch(
            @PathVariable Long id, @Valid @RequestBody SavedSearchRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved search updated successfully", savedSearchService.update(id, request, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSavedSearch(@PathVariable Long id, Authentication authentication) {
        savedSearchService.delete(id, currentUser(authentication));
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved search deleted successfully", null));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<SavedSearchResponse>> pauseSavedSearch(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved search paused", savedSearchService.setEnabled(id, false, currentUser(authentication))));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<SavedSearchResponse>> resumeSavedSearch(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved search resumed", savedSearchService.setEnabled(id, true, currentUser(authentication))));
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new RuntimeException("Authenticated user not found");
        }
        return user;
    }
}