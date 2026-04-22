package com.job_Portal_Backend.job_portal_backend.bookmarks.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookmarkToggleResponse {
    private boolean bookmarked;
    private Long jobId;
}
