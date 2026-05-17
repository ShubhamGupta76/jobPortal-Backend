package com.job_Portal_Backend.job_portal_backend.assessments.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingTestCase {
    private String input;
    private String expectedOutput;
}
