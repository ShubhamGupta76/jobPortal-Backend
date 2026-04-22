package com.job_Portal_Backend.job_portal_backend.companies.controller;

import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyRequest;
import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyResponse;
import com.job_Portal_Backend.job_portal_backend.companies.service.CompanyService;
import com.job_Portal_Backend.job_portal_backend.config.JwtService;
import com.job_Portal_Backend.job_portal_backend.dto.ApiResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@CrossOrigin(origins = "*")
public class CompanyController {

    private final CompanyService companyService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public CompanyController(CompanyService companyService, JwtService jwtService, UserRepository userRepository) {
        this.companyService = companyService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getCompanies() {
        return ResponseEntity
                .ok(new ApiResponse<>(true, "Companies retrieved successfully", companyService.getAllCompanies()));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> getMyCompany(@RequestHeader("Authorization") String token) {
        CompanyResponse company = companyService.getMyCompany(resolveUser(token));
        if (company == null) {
            return ResponseEntity.ok(new ApiResponse<>(true, "No company profile found", null));
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "Company profile retrieved successfully", company));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CompanyRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Company created successfully",
                companyService.createCompany(request, resolveUser(token))));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateMyCompany(
            @Valid @RequestBody CompanyRequest request,
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Company updated successfully",
                companyService.updateMyCompany(request, resolveUser(token))));
    }

    private User resolveUser(String token) {
        String email = jwtService.extractUsername(token.substring(7));
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
