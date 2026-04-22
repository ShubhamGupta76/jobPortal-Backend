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
public class FileUploadDto {
    private Long id;
    private Long userId;
    private String userEmail;
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String entityType;
    private Long entityId;
    private String fileCategory;
    private LocalDateTime uploadedAt;
}