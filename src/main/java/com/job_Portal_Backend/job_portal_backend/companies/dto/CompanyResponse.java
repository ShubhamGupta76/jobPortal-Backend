package com.job_Portal_Backend.job_portal_backend.companies.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyResponse {
    private Long id;
    private String name;
    private String description;
    private String website;
    private String location;
    private String industry;
    private String size;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
