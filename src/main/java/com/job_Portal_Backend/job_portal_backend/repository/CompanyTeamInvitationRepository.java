package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamInvitation;
import com.job_Portal_Backend.job_portal_backend.entity.CompanyTeamInvitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyTeamInvitationRepository extends JpaRepository<CompanyTeamInvitation, Long> {

    List<CompanyTeamInvitation> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    Optional<CompanyTeamInvitation> findByToken(String token);

    boolean existsByCompanyIdAndEmailIgnoreCaseAndStatus(Long companyId, String email, InvitationStatus status);
}
