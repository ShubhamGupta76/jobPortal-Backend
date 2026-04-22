package com.job_Portal_Backend.job_portal_backend.bookmarks.service;

import com.job_Portal_Backend.job_portal_backend.bookmarks.dto.BookmarkToggleResponse;
import com.job_Portal_Backend.job_portal_backend.entity.Bookmark;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.jobs.dto.JobDto;
import com.job_Portal_Backend.job_portal_backend.mapper.JobMapper;
import com.job_Portal_Backend.job_portal_backend.service.NotificationService;
import com.job_Portal_Backend.job_portal_backend.repository.BookmarkRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final NotificationService notificationService;

    public BookmarkServiceImpl(
            BookmarkRepository bookmarkRepository,
            JobRepository jobRepository,
            JobMapper jobMapper,
            NotificationService notificationService) {
        this.bookmarkRepository = bookmarkRepository;
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
        this.notificationService = notificationService;
    }

    @Override
    public BookmarkToggleResponse toggleBookmark(Long jobId, User user) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return bookmarkRepository.findByUserIdAndJobId(user.getId(), jobId)
                .map(existing -> {
                    bookmarkRepository.delete(existing);
                    return new BookmarkToggleResponse(false, jobId);
                })
                .orElseGet(() -> {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setUser(user);
                    bookmark.setJob(job);
                    bookmarkRepository.save(bookmark);
                    notificationService.sendNotificationToUser(
                            user.getId(),
                            "BOOKMARK",
                            "Job saved",
                            "You saved " + job.getTitle() + " for later review.",
                            job.getId().toString());
                    return new BookmarkToggleResponse(true, jobId);
                });
    }

    @Override
    public List<JobDto> getSavedJobs(User user) {
        return bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(Bookmark::getJob)
                .map(jobMapper::toDto)
                .toList();
    }

    @Override
    public boolean isBookmarked(Long jobId, User user) {
        return bookmarkRepository.existsByUserIdAndJobId(user.getId(), jobId);
    }

    @Override
    public long countSavedJobs(User user) {
        return bookmarkRepository.countByUserId(user.getId());
    }
}
