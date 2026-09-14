package com.job_Portal_Backend.job_portal_backend.companies.service;

import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyRequest;
import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyResponse;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyVerification;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyVerificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyVerificationRepository companyVerificationRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository,
            CompanyVerificationRepository companyVerificationRepository) {
        this.companyRepository = companyRepository;
        this.companyVerificationRepository = companyVerificationRepository;
    }

    @Override
    public CompanyResponse createCompany(CompanyRequest request, User owner) {
        Company company = companyRepository.findByOwnerId(owner.getId()).orElseGet(Company::new);
        apply(company, request, owner);
        Company saved = companyRepository.save(company);
        return toDto(saved, verificationStatusFor(saved.getId()));
    }

    @Override
    public CompanyResponse updateMyCompany(CompanyRequest request, User owner) {
        Company company = companyRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found"));
        apply(company, request, owner);
        Company saved = companyRepository.save(company);
        return toDto(saved, verificationStatusFor(saved.getId()));
    }

    @Override
    public CompanyResponse getMyCompany(User owner) {
        return companyRepository.findByOwnerId(owner.getId())
                .map(company -> toDto(company, verificationStatusFor(company.getId())))
                .orElse(null);
    }

    @Override
    public List<CompanyResponse> getMyCompanies(User owner) {
        return companyRepository.findAllByOwnerId(owner.getId()).stream()
                .map(company -> toDto(company, verificationStatusFor(company.getId())))
                .toList();
    }

    private String verificationStatusFor(Long companyId) {
        return companyVerificationRepository.findByCompanyId(companyId)
                .map(v -> v.getStatus().name())
                .orElse("UNVERIFIED");
    }

    @Override
    public List<CompanyResponse> getAllCompanies() {
        List<Company> companies = companyRepository.findAll();
        Map<Long, String> statusByCompanyId = companyVerificationRepository.findAll().stream()
                .collect(Collectors.toMap(v -> v.getCompany().getId(), v -> v.getStatus().name()));
        return companies.stream().map(company -> toDto(company, statusByCompanyId.get(company.getId()))).toList();
    }

    private void apply(Company company, CompanyRequest request, User owner) {
        company.setName(request.getName());
        company.setDescription(request.getDescription());
        company.setWebsite(request.getWebsite());
        company.setLocation(request.getLocation());
        company.setIndustry(request.getIndustry());
        company.setSize(request.getSize());
        company.setOwner(owner);
    }

    private CompanyResponse toDto(Company company, String verificationStatus) {
        CompanyResponse dto = new CompanyResponse();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setDescription(company.getDescription());
        dto.setWebsite(company.getWebsite());
        dto.setLocation(company.getLocation());
        dto.setIndustry(company.getIndustry());
        dto.setSize(company.getSize());
        if (company.getOwner() != null) {
            dto.setOwnerId(company.getOwner().getId());
            dto.setOwnerName((company.getOwner().getFirstName() + " " + company.getOwner().getLastName()).trim());
        }
        dto.setCreatedAt(company.getCreatedAt());
        dto.setUpdatedAt(company.getUpdatedAt());
        dto.setVerificationStatus(verificationStatus != null ? verificationStatus : "UNVERIFIED");
        return dto;
    }
}
