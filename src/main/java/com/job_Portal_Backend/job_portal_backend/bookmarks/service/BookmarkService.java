package com.job_Portal_Backend.job_portal_backend.bookmarks.service;

import com.job_Portal_Backend.job_portal_backend.bookmarks.dto.BookmarkToggleResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;

import java.util.List;

public interface BookmarkService {
    BookmarkToggleResponse toggleBookmark(Long jobId, User user);
    List<JobDto> getSavedJobs(User user);
    boolean isBookmarked(Long jobId, User user);
    long countSavedJobs(User user);
}
