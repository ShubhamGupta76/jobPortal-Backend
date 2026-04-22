package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByOwnerId(Long ownerId);

    @Query("SELECT c FROM Company c WHERE c.owner.id = :ownerId AND c.isDeleted = false")
    Optional<Company> findByOwnerIdAndNotDeleted(@Param("ownerId") Long ownerId);

    List<Company> findAllByOwnerId(Long ownerId);

    @Query("SELECT c FROM Company c WHERE c.owner.id = :ownerId AND c.isDeleted = false")
    List<Company> findAllByOwnerIdAndNotDeleted(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Company c WHERE c.isDeleted = false")
    List<Company> findAllNotDeleted();
}
