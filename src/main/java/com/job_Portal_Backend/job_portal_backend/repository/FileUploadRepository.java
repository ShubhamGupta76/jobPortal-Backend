package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.FileUpload;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    Optional<FileUpload> findByIdAndUserAndIsDeletedFalse(Long id, User user);

    List<FileUpload> findByEntityTypeAndEntityIdAndUserAndIsDeletedFalse(String entityType, Long entityId, User user);

    List<FileUpload> findByUserAndIsDeletedFalse(User user);

    List<FileUpload> findByEntityTypeAndEntityIdAndIsDeletedFalse(String entityType, Long entityId);

    List<FileUpload> findByUserAndEntityTypeAndIsDeletedFalse(User user, String entityType);

    Optional<FileUpload> findByStoredFilenameAndIsDeletedFalse(String storedFilename);

    Page<FileUpload> findByUserAndIsDeletedFalse(User user, Pageable pageable);

    @Query("SELECT f FROM FileUpload f WHERE f.user = :user AND f.fileCategory = :category AND f.isDeleted = false ORDER BY f.uploadedAt DESC")
    List<FileUpload> findByUserAndCategory(@Param("user") User user, @Param("category") String category);

    @Query("SELECT f FROM FileUpload f WHERE f.contentType LIKE :contentTypePattern AND f.isDeleted = false")
    List<FileUpload> findByContentTypePattern(@Param("contentTypePattern") String contentTypePattern);

    @Query("SELECT SUM(f.fileSize) FROM FileUpload f WHERE f.user = :user AND f.isDeleted = false")
    Long getTotalFileSizeByUser(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM FileUpload f WHERE f.user = :user AND f.isDeleted = false")
    long countByUserAndNotDeleted(@Param("user") User user);

    @Query("SELECT f FROM FileUpload f WHERE f.isDeleted = true AND f.uploadedAt < :cutoffDate")
    List<FileUpload> findSoftDeletedFilesOlderThan(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}