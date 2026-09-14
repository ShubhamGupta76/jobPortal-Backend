package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.CreditLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditLedgerEntryRepository extends JpaRepository<CreditLedgerEntry, Long> {

    Page<CreditLedgerEntry> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    @Query("SELECT e FROM CreditLedgerEntry e WHERE e.company.id = :companyId ORDER BY e.id DESC LIMIT 1")
    Optional<CreditLedgerEntry> findLatestByCompanyId(@Param("companyId") Long companyId);

    boolean existsByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
