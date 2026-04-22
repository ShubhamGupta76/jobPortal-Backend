package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.dto.InterviewCreateDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewDto;
import com.job_Portal_Backend.job_portal_backend.dto.InterviewUpdateDto;
import com.job_Portal_Backend.job_portal_backend.entity.Interview;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.InterviewRepository;
import com.job_Portal_Backend.job_portal_backend.repository.JobRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import com.job_Portal_Backend.job_portal_backend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    public InterviewDto createInterview(InterviewCreateDto createDto, User recruiter) {
        Job job = jobRepository.findById(createDto.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalArgumentException("Only the job recruiter can schedule interviews");
        }

        User candidate = userRepository.findById(createDto.getCandidateId())
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        // Check for interview conflicts
        if (hasInterviewConflict(candidate, createDto.getScheduledAt(),
                createDto.getScheduledAt().plusMinutes(createDto.getDurationMinutes()), null)) {
            throw new IllegalArgumentException("Candidate has a conflicting interview at this time");
        }

        if (hasInterviewConflict(recruiter, createDto.getScheduledAt(),
                createDto.getScheduledAt().plusMinutes(createDto.getDurationMinutes()), null)) {
            throw new IllegalArgumentException("Recruiter has a conflicting interview at this time");
        }

        Interview interview = new Interview();
        interview.setJob(job);
        interview.setCandidate(candidate);
        interview.setRecruiter(recruiter);
        interview.setScheduledAt(createDto.getScheduledAt());
        interview.setType(Interview.InterviewType.valueOf(createDto.getType()));
        interview.setNotes(createDto.getNotes());
        interview.setMeetingLink(createDto.getMeetingLink());
        interview.setLocation(createDto.getLocation());
        interview.setDurationMinutes(createDto.getDurationMinutes());
        interview.setStatus(Interview.InterviewStatus.SCHEDULED);

        Interview savedInterview = interviewRepository.save(interview);
        return mapToDto(savedInterview);
    }

    @Override
    public InterviewDto updateInterview(Long interviewId, InterviewUpdateDto updateDto, User currentUser) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getRecruiter().getId().equals(currentUser.getId()) &&
                !interview.getCandidate().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized to update this interview");
        }

        if (updateDto.getScheduledAt() != null) {
            interview.setScheduledAt(updateDto.getScheduledAt());
        }
        if (updateDto.getType() != null) {
            interview.setType(Interview.InterviewType.valueOf(updateDto.getType()));
        }
        if (updateDto.getStatus() != null) {
            interview.setStatus(Interview.InterviewStatus.valueOf(updateDto.getStatus()));
        }
        if (updateDto.getNotes() != null) {
            interview.setNotes(updateDto.getNotes());
        }
        if (updateDto.getMeetingLink() != null) {
            interview.setMeetingLink(updateDto.getMeetingLink());
        }
        if (updateDto.getLocation() != null) {
            interview.setLocation(updateDto.getLocation());
        }
        if (updateDto.getDurationMinutes() != null) {
            interview.setDurationMinutes(updateDto.getDurationMinutes());
        }

        Interview updatedInterview = interviewRepository.save(interview);
        return mapToDto(updatedInterview);
    }

    @Override
    public InterviewDto getInterviewById(Long interviewId, User currentUser) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getRecruiter().getId().equals(currentUser.getId()) &&
                !interview.getCandidate().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized to view this interview");
        }

        return mapToDto(interview);
    }

    @Override
    public void deleteInterview(Long interviewId, User currentUser) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getRecruiter().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Only the recruiter can delete interviews");
        }

        interview.setIsDeleted(true);
        interviewRepository.save(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewDto> getInterviewsByCandidate(User candidate, Pageable pageable) {
        Page<Interview> interviews = interviewRepository.findByCandidateAndIsDeletedFalse(candidate, pageable);
        return interviews.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InterviewDto> getInterviewsByRecruiter(User recruiter, Pageable pageable) {
        Page<Interview> interviews = interviewRepository.findByRecruiterAndIsDeletedFalse(recruiter, pageable);
        return interviews.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getUpcomingInterviewsByCandidate(User candidate) {
        List<Interview> interviews = interviewRepository.findUpcomingInterviewsByCandidate(candidate,
                LocalDateTime.now());
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getUpcomingInterviewsByRecruiter(User recruiter) {
        List<Interview> interviews = interviewRepository.findUpcomingInterviewsByRecruiter(recruiter,
                LocalDateTime.now());
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getInterviewsByJob(Long jobId, User currentUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.getRecruiter().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized to view interviews for this job");
        }

        List<Interview> interviews = interviewRepository.findByJobIdAndIsDeletedFalse(jobId);
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public InterviewDto confirmInterview(Long interviewId, User candidate) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getCandidate().getId().equals(candidate.getId())) {
            throw new IllegalArgumentException("Only the candidate can confirm interviews");
        }

        if (interview.getStatus() != Interview.InterviewStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only scheduled interviews can be confirmed");
        }

        interview.setStatus(Interview.InterviewStatus.CONFIRMED);
        Interview updatedInterview = interviewRepository.save(interview);
        return mapToDto(updatedInterview);
    }

    @Override
    public InterviewDto cancelInterview(Long interviewId, User currentUser) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getRecruiter().getId().equals(currentUser.getId()) &&
                !interview.getCandidate().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Unauthorized to cancel this interview");
        }

        interview.setStatus(Interview.InterviewStatus.CANCELLED);
        Interview updatedInterview = interviewRepository.save(interview);
        return mapToDto(updatedInterview);
    }

    @Override
    public InterviewDto markAsCompleted(Long interviewId, User recruiter) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalArgumentException("Only the recruiter can mark interviews as completed");
        }

        interview.setStatus(Interview.InterviewStatus.COMPLETED);
        Interview updatedInterview = interviewRepository.save(interview);
        return mapToDto(updatedInterview);
    }

    @Override
    public InterviewDto markAsNoShow(Long interviewId, User recruiter) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getRecruiter().getId().equals(recruiter.getId())) {
            throw new IllegalArgumentException("Only the recruiter can mark interviews as no-show");
        }

        interview.setStatus(Interview.InterviewStatus.NO_SHOW);
        Interview updatedInterview = interviewRepository.save(interview);
        return mapToDto(updatedInterview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewDto> getOverdueInterviews() {
        List<Interview> interviews = interviewRepository.findOverdueInterviews(LocalDateTime.now());
        return interviews.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countInterviewsByRecruiterAndDateRange(User recruiter, LocalDateTime start, LocalDateTime end) {
        return interviewRepository.countInterviewsByRecruiterAndDateRange(recruiter, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasInterviewConflict(User user, LocalDateTime startTime, LocalDateTime endTime,
            Long excludeInterviewId) {
        // Check interviews where user is a candidate
        List<Interview> candidateInterviews = interviewRepository.findByCandidateAndIsDeletedFalse(user);
        // Check interviews where user is a recruiter
        List<Interview> recruiterInterviews = interviewRepository.findByRecruiterAndIsDeletedFalse(user);

        // Combine both lists
        List<Interview> allUserInterviews = new ArrayList<>();
        allUserInterviews.addAll(candidateInterviews);
        allUserInterviews.addAll(recruiterInterviews);

        for (Interview interview : allUserInterviews) {
            if (excludeInterviewId != null && interview.getId().equals(excludeInterviewId)) {
                continue;
            }

            if (interview.getStatus() == Interview.InterviewStatus.CANCELLED ||
                    interview.getStatus() == Interview.InterviewStatus.COMPLETED) {
                continue;
            }

            LocalDateTime interviewStart = interview.getScheduledAt();
            LocalDateTime interviewEnd = interviewStart
                    .plusMinutes(interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 60);

            if (startTime.isBefore(interviewEnd) && endTime.isAfter(interviewStart)) {
                return true;
            }
        }

        return false;
    }

    private InterviewDto mapToDto(Interview interview) {
        return InterviewDto.builder()
                .id(interview.getId())
                .jobId(interview.getJob().getId())
                .jobTitle(interview.getJob().getTitle())
                .candidateId(interview.getCandidate().getId())
                .candidateName(interview.getCandidate().getFirstName() + " " + interview.getCandidate().getLastName())
                .candidateEmail(interview.getCandidate().getEmail())
                .recruiterId(interview.getRecruiter().getId())
                .recruiterName(interview.getRecruiter().getFirstName() + " " + interview.getRecruiter().getLastName())
                .recruiterEmail(interview.getRecruiter().getEmail())
                .scheduledAt(interview.getScheduledAt())
                .type(interview.getType().toString())
                .status(interview.getStatus().toString())
                .notes(interview.getNotes())
                .meetingLink(interview.getMeetingLink())
                .location(interview.getLocation())
                .durationMinutes(interview.getDurationMinutes())
                .createdAt(interview.getCreatedAt())
                .updatedAt(interview.getUpdatedAt())
                .build();
    }
}