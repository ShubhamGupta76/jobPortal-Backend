package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findByCompanyIdOrderByIssueDateDesc(Long companyId, Pageable pageable);

    Optional<Invoice> findByIdAndCompanyId(Long id, Long companyId);

    Optional<Invoice> findByTransactionId(Long transactionId);
}
