package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamMember;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamMember.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyTeamMemberRepository extends JpaRepository<CompanyTeamMember, Long> {

    List<CompanyTeamMember> findByCompanyIdOrderByCreatedAtAsc(Long companyId);

    Optional<CompanyTeamMember> findByCompanyIdAndUserId(Long companyId, Long userId);

    long countByCompanyIdAndRole(Long companyId, TeamRole role);

    List<CompanyTeamMember> findByCompanyIdAndRole(Long companyId, TeamRole role);

    long countByCompanyId(Long companyId);
}
