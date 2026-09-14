package com.job_Portal_Backend.job_portal_backend.notificationpreferences.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NotificationPreferenceDto {

    @NotBlank(message = "Category is required")
    private String category;

    private boolean inAppEnabled;
    private boolean emailEnabled;

    public NotificationPreferenceDto(String category, boolean inAppEnabled, boolean emailEnabled) {
        this.category = category;
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
    }
}
