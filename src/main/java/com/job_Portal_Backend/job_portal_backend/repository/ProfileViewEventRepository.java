package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.ProfileViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ProfileViewEventRepository extends JpaRepository<ProfileViewEvent, Long> {

    long countByViewedUserId(Long viewedUserId);

    long countByViewedUserIdAndCreatedAtAfter(Long viewedUserId, LocalDateTime since);

    @Query("SELECT COUNT(DISTINCT e.viewedBy.id) FROM ProfileViewEvent e WHERE e.viewedUser.id = :viewedUserId AND e.createdAt >= :since")
    long countDistinctViewersSince(@Param("viewedUserId") Long viewedUserId, @Param("since") LocalDateTime since);

    List<ProfileViewEvent> findTop20ByViewedUserIdOrderByCreatedAtDesc(Long viewedUserId);
}
