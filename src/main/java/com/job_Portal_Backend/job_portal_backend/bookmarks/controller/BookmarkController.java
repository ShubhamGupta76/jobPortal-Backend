package com.job_Portal_Backend.job_portal_backend.bookmarks.controller;

import com.job_Portal_Backend.job_portal_backend.bookmarks.dto.BookmarkToggleResponse;
import com.job_Portal_Backend.job_portal_backend.bookmarks.service.BookmarkService;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookmarks")
@CrossOrigin(origins = "*")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public BookmarkController(BookmarkService bookmarkService, JwtService jwtService, UserRepository userRepository) {
        this.bookmarkService = bookmarkService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/{jobId}/toggle")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<BookmarkToggleResponse>> toggleBookmark(
            @PathVariable Long jobId,
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bookmark status updated", bookmarkService.toggleBookmark(jobId, resolveUser(token))));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<List<JobDto>>> getSavedJobs(@RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Saved jobs retrieved successfully", bookmarkService.getSavedJobs(resolveUser(token))));
    }

    @GetMapping("/{jobId}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Boolean>> getBookmarkStatus(
            @PathVariable Long jobId,
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Bookmark status retrieved", bookmarkService.isBookmarked(jobId, resolveUser(token))));
    }

    private User resolveUser(String token) {
        String email = jwtService.extractUsername(token.substring(7));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
