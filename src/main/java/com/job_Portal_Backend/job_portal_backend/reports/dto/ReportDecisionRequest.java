package com.job_Portal_Backend.job_portal_backend.reports.dto;

import lombok.Data;

@Data
public class ReportDecisionRequest {
    private String note;
    private boolean suspendTarget;
}
