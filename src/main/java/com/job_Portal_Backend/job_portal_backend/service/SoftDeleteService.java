package com.job_Portal_Backend.job_portal_backend.service;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import java.time.LocalDateTime;
import java.util.List;

public interface SoftDeleteService {

    <T> void softDelete(T entity, User deletedBy);

    <T> void restore(T entity, User restoredBy);

    <T> boolean isDeleted(T entity);

    <T> LocalDateTime getDeletedAt(T entity);

    <T> User getDeletedBy(T entity);

    void permanentlyDeleteOldRecords(int daysOld);

    <T> List<T> findDeletedRecords(Class<T> entityClass);

    <T> List<T> findDeletedRecordsSince(Class<T> entityClass, LocalDateTime since);

    <T> long countDeletedRecords(Class<T> entityClass);

    <T> long countDeletedRecordsSince(Class<T> entityClass, LocalDateTime since);

    void validateDeletePermission(Object entity, User user);

    void validateRestorePermission(Object entity, User user);

    boolean canDelete(Object entity, User user);

    boolean canRestore(Object entity, User user);

    void softDeleteUser(Long userId);

    void softDeleteJob(Long jobId);

    void softDeleteApplication(Long applicationId);

    void softDeleteCompany(Long companyId);

    void softDeleteInterview(Long interviewId);

    void softDeleteFile(Long fileId);

    void restoreUser(Long userId);

    void restoreJob(Long jobId);

    void restoreApplication(Long applicationId);

    void restoreCompany(Long companyId);

    void restoreInterview(Long interviewId);

    void restoreFile(Long fileId);

    boolean isEntityDeleted(String entityType, Long entityId);
}