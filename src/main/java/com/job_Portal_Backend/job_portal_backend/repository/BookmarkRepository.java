package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserIdAndJobId(Long userId, Long jobId);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);

    // JOIN FETCH avoids one extra SELECT per bookmark for every caller that reads job fields
    // (all of them do) instead of just the job id.
    @Query("SELECT b FROM Bookmark b JOIN FETCH b.job WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Bookmark> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    long countByUserId(Long userId);
    void deleteByUserIdAndJobId(Long userId, Long jobId);
}
