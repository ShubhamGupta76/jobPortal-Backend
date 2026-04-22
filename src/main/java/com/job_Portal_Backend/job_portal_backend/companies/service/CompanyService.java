package com.job_Portal_Backend.job_portal_backend.companies.service;

import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyRequest;
import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyResponse;
import com.job_Portal_Backend.job_portal_backend.entity.User;

import java.util.List;

public interface CompanyService {
    CompanyResponse createCompany(CompanyRequest request, User owner);
    CompanyResponse updateMyCompany(CompanyRequest request, User owner);
    CompanyResponse getMyCompany(User owner);
    List<CompanyResponse> getMyCompanies(User owner);
    List<CompanyResponse> getAllCompanies();
}
