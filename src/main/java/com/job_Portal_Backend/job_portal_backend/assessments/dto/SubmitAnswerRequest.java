package com.job_Portal_Backend.job_portal_backend.assessments.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    @NotNull
    @NotEmpty
    @NotBlank(message = "Session token is required")
    private String sessionToken;

    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String answerText;
    private String codeSubmitted;
    private String language;
    private Boolean evaluate;
}
