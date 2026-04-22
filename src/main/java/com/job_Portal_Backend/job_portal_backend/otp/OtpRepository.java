package com.job_Portal_Backend.job_portal_backend.otp;

import com.job_Portal_Backend.job_portal_backend.otp.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByEmailAndVerifiedFalseAndExpiryTimeAfter(String email, LocalDateTime now);

    boolean existsByEmailAndExpiryTimeAfter(String email, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiryTime < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    Optional<Otp> findTopByEmailAndExpiryTimeAfterOrderByCreatedAtDesc(String email, LocalDateTime now);
}
