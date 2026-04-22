package com.job_Portal_Backend.job_portal_backend.companies.service;

import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyRequest;
import com.job_Portal_Backend.job_portal_backend.companies.dto.CompanyResponse;
import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.exception.ResourceNotFoundException;
import com.job_Portal_Backend.job_portal_backend.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public CompanyResponse createCompany(CompanyRequest request, User owner) {
        Company company = companyRepository.findByOwnerId(owner.getId()).orElseGet(Company::new);
        apply(company, request, owner);
        return toDto(companyRepository.save(company));
    }

    @Override
    public CompanyResponse updateMyCompany(CompanyRequest request, User owner) {
        Company company = companyRepository.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found"));
        apply(company, request, owner);
        return toDto(companyRepository.save(company));
    }

    @Override
    public CompanyResponse getMyCompany(User owner) {
        return companyRepository.findByOwnerId(owner.getId())
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public List<CompanyResponse> getMyCompanies(User owner) {
        return companyRepository.findAllByOwnerId(owner.getId()).stream().map(this::toDto).toList();
    }

    @Override
    public List<CompanyResponse> getAllCompanies() {
        return companyRepository.findAll().stream().map(this::toDto).toList();
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

    private CompanyResponse toDto(Company company) {
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
        return dto;
    }
}
