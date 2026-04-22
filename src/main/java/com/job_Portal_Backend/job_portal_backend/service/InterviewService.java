package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.dto.InterviewCreateDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewUpdateDto;
import com.job_Portal_Backend.job_portal_backend.entity.Interview;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface InterviewService {

    InterviewDto createInterview(InterviewCreateDto createDto, User recruiter);

    InterviewDto updateInterview(Long interviewId, InterviewUpdateDto updateDto, User currentUser);

    InterviewDto getInterviewById(Long interviewId, User currentUser);

    void deleteInterview(Long interviewId, User currentUser);

    Page<InterviewDto> getInterviewsByCandidate(User candidate, Pageable pageable);

    Page<InterviewDto> getInterviewsByRecruiter(User recruiter, Pageable pageable);

    List<InterviewDto> getUpcomingInterviewsByCandidate(User candidate);

    List<InterviewDto> getUpcomingInterviewsByRecruiter(User recruiter);

    List<InterviewDto> getInterviewsByJob(Long jobId, User currentUser);

    InterviewDto confirmInterview(Long interviewId, User candidate);

    InterviewDto cancelInterview(Long interviewId, User currentUser);

    InterviewDto markAsCompleted(Long interviewId, User recruiter);

    InterviewDto markAsNoShow(Long interviewId, User recruiter);

    List<InterviewDto> getOverdueInterviews();

    long countInterviewsByRecruiterAndDateRange(User recruiter, LocalDateTime start, LocalDateTime end);

    boolean hasInterviewConflict(User user, LocalDateTime startTime, LocalDateTime endTime, Long excludeInterviewId);
}