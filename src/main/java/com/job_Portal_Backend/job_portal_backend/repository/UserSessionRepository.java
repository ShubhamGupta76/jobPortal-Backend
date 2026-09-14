package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash);

    Optional<UserSession> findByPreviousRefreshTokenHash(String previousRefreshTokenHash);

    Optional<UserSession> findByIdAndUser(Long id, User user);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.revokedAt IS NULL AND s.expiresAt > :now ORDER BY s.lastUsedAt DESC")
    List<UserSession> findActiveByUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.revokedAt IS NULL AND s.id <> :exceptSessionId")
    List<UserSession> findNonRevokedByUserExcept(@Param("user") User user, @Param("exceptSessionId") Long exceptSessionId);

    @Query("SELECT s FROM UserSession s WHERE s.user = :user AND s.revokedAt IS NULL")
    List<UserSession> findNonRevokedByUser(@Param("user") User user);
}
