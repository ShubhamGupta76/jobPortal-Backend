package com.job_Portal_Backend.job_portal_backend.service.impl;

import com.job_Portal_Backend.job_portal_backend.entity.*;
import com.job_Portal_Backend.job_portal_backend.repository.*;
import com.job_Portal_Backend.job_portal_backend.service.SoftDeleteService;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SoftDeleteServiceImpl implements SoftDeleteService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Override
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Soft delete related applications
        List<Application> userApplications = applicationRepository.findByUserIdAndNotDeleted(user.getId());
        for (Application application : userApplications) {
            softDeleteApplication(application.getId());
        }

        // Soft delete related interviews (as candidate)
        List<Interview> userInterviews = interviewRepository.findByCandidateAndIsDeletedFalse(user);
        for (Interview interview : userInterviews) {
            softDeleteInterview(interview.getId());
        }

        // Soft delete related interviews (as recruiter)
        List<Interview> recruiterInterviews = interviewRepository.findByRecruiterAndIsDeletedFalse(user);
        for (Interview interview : recruiterInterviews) {
            softDeleteInterview(interview.getId());
        }

        // Soft delete related file uploads
        List<FileUpload> userFiles = fileUploadRepository.findByUserAndIsDeletedFalse(user);
        for (FileUpload file : userFiles) {
            softDeleteFile(file.getId());
        }

        // Soft delete the user
        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void softDeleteJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        // Soft delete related applications
        List<Application> jobApplications = applicationRepository.findByJobIdAndNotDeleted(job.getId());
        for (Application application : jobApplications) {
            softDeleteApplication(application.getId());
        }

        // Soft delete related interviews
        List<Interview> jobInterviews = interviewRepository.findByJobIdAndIsDeletedFalse(job.getId());
        for (Interview interview : jobInterviews) {
            softDeleteInterview(interview.getId());
        }

        // Soft delete the job
        job.setIsDeleted(true);
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void softDeleteApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        // Soft delete related interviews for the application candidate and job
        List<Interview> applicationInterviews = interviewRepository.findByCandidateAndJobAndIsDeletedFalse(
                application.getUser(), application.getJob());
        for (Interview interview : applicationInterviews) {
            softDeleteInterview(interview.getId());
        }

        // Soft delete the application
        application.setIsDeleted(true);
        applicationRepository.save(application);
    }

    @Override
    @Transactional
    public void softDeleteCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        // Soft delete related jobs
        List<Job> companyJobs = jobRepository.findByCompanyAndIsDeletedFalse(company);
        for (Job job : companyJobs) {
            softDeleteJob(job.getId());
        }

        // Soft delete the company
        company.setIsDeleted(true);
        companyRepository.save(company);
    }

    @Override
    @Transactional
    public void softDeleteInterview(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));

        interview.setIsDeleted(true);
        interviewRepository.save(interview);
    }

    @Override
    @Transactional
    public void softDeleteFile(Long fileId) {
        FileUpload file = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        file.setIsDeleted(true);
        fileUploadRepository.save(file);
    }

    @Override
    @Transactional
    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsDeleted(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void restoreJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        job.setIsDeleted(false);
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void restoreApplication(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        application.setIsDeleted(false);
        applicationRepository.save(application);
    }

    @Override
    @Transactional
    public void restoreCompany(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        company.setIsDeleted(false);
        companyRepository.save(company);
    }

    @Override
    @Transactional
    public void restoreInterview(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found"));

        interview.setIsDeleted(false);
        interviewRepository.save(interview);
    }

    @Override
    @Transactional
    public void restoreFile(Long fileId) {
        FileUpload file = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        file.setIsDeleted(false);
        fileUploadRepository.save(file);
    }

    @Override
    public void validateDeletePermission(Object entity, User user) {
        // Add permission checks here if needed.
    }

    @Override
    public void validateRestorePermission(Object entity, User user) {
        // Add permission checks here if needed.
    }

    @Override
    public boolean canDelete(Object entity, User user) {
        return true;
    }

    @Override
    public boolean canRestore(Object entity, User user) {
        return true;
    }

    @Override
    public boolean isEntityDeleted(String entityType, Long entityId) {
        switch (entityType.toLowerCase()) {
            case "user":
                return userRepository.findById(entityId)
                        .map(User::getIsDeleted)
                        .orElse(true);
            case "job":
                return jobRepository.findById(entityId)
                        .map(Job::getIsDeleted)
                        .orElse(true);
            case "application":
                return applicationRepository.findById(entityId)
                        .map(Application::getIsDeleted)
                        .orElse(true);
            case "company":
                return companyRepository.findById(entityId)
                        .map(Company::getIsDeleted)
                        .orElse(true);
            case "interview":
                return interviewRepository.findById(entityId)
                        .map(Interview::getIsDeleted)
                        .orElse(true);
            case "file":
                return fileUploadRepository.findById(entityId)
                        .map(FileUpload::getIsDeleted)
                        .orElse(true);
            default:
                return true;
        }
    }

    @Override
    public <T> void softDelete(T entity, User deletedBy) {
        throw new UnsupportedOperationException(
                "Generic softDelete is not implemented. Use type-specific methods instead.");
    }

    @Override
    public <T> void restore(T entity, User restoredBy) {
        throw new UnsupportedOperationException(
                "Generic restore is not implemented. Use type-specific methods instead.");
    }

    @Override
    public <T> boolean isDeleted(T entity) {
        if (entity instanceof User) {
            return Boolean.TRUE.equals(((User) entity).getIsDeleted());
        }
        if (entity instanceof Job) {
            return Boolean.TRUE.equals(((Job) entity).getIsDeleted());
        }
        if (entity instanceof Application) {
            return Boolean.TRUE.equals(((Application) entity).getIsDeleted());
        }
        if (entity instanceof Company) {
            return Boolean.TRUE.equals(((Company) entity).getIsDeleted());
        }
        if (entity instanceof Interview) {
            return Boolean.TRUE.equals(((Interview) entity).getIsDeleted());
        }
        if (entity instanceof FileUpload) {
            return Boolean.TRUE.equals(((FileUpload) entity).getIsDeleted());
        }
        return false;
    }

    @Override
    public <T> LocalDateTime getDeletedAt(T entity) {
        throw new UnsupportedOperationException("getDeletedAt is not implemented for generic entity types.");
    }

    @Override
    public <T> User getDeletedBy(T entity) {
        throw new UnsupportedOperationException("getDeletedBy is not implemented for generic entity types.");
    }

    @Override
    public void permanentlyDeleteOldRecords(int daysOld) {
        throw new UnsupportedOperationException("permanentlyDeleteOldRecords is not implemented.");
    }

    @Override
    public <T> List<T> findDeletedRecords(Class<T> entityClass) {
        throw new UnsupportedOperationException("findDeletedRecords is not implemented.");
    }

    @Override
    public <T> List<T> findDeletedRecordsSince(Class<T> entityClass, LocalDateTime since) {
        throw new UnsupportedOperationException("findDeletedRecordsSince is not implemented.");
    }

    @Override
    public <T> long countDeletedRecords(Class<T> entityClass) {
        throw new UnsupportedOperationException("countDeletedRecords is not implemented.");
    }

    @Override
    public <T> long countDeletedRecordsSince(Class<T> entityClass, LocalDateTime since) {
        throw new UnsupportedOperationException("countDeletedRecordsSince is not implemented.");
    }
}
