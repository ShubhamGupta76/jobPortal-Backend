package com.job_Portal_Backend.job_portal_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewCreateDto {

    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    @NotNull(message = "Recruiter ID is required")
    private Long recruiterId;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledAt;

    @NotBlank(message = "Interview type is required")
    private String type; // TECHNICAL, HR, SYSTEM_DESIGN, CODING

    private String notes;

    private String meetingLink;

    private String location;

    private Integer durationMinutes;
}