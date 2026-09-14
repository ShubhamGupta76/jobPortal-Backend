package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Page<PaymentTransaction> findByCompanyIdOrderByCreatedAtDesc(Long companyId, Pageable pageable);

    Optional<PaymentTransaction> findByIdAndCompanyId(Long id, Long companyId);

    Optional<PaymentTransaction> findByProviderAndProviderTransactionId(String provider, String providerTransactionId);

    Optional<PaymentTransaction> findByIdempotencyKey(String idempotencyKey);
}
