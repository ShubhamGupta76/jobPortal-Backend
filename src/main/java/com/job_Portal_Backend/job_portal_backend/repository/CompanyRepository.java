package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByOwnerId(Long ownerId);

    // Serializes concurrent billing/credit-ledger mutations for a company; see
    // CreditLedgerService. Not used for any non-billing read/write path.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Company c WHERE c.id = :id")
    Optional<Company> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT c FROM Company c WHERE c.owner.id = :ownerId AND c.isDeleted = false")
    Optional<Company> findByOwnerIdAndNotDeleted(@Param("ownerId") Long ownerId);

    List<Company> findAllByOwnerId(Long ownerId);

    @Query("SELECT c FROM Company c WHERE c.owner.id = :ownerId AND c.isDeleted = false")
    List<Company> findAllByOwnerIdAndNotDeleted(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Company c WHERE c.isDeleted = false")
    List<Company> findAllNotDeleted();
}
