package com.job_Portal_Backend.job_portal_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private LocalDateTime scheduledAt;
    private String type; // TECHNICAL, HR, SYSTEM_DESIGN, CODING
    private String status; // SCHEDULED, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
    private String notes;
    private String meetingLink;
    private String location;
    private Integer durationMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}