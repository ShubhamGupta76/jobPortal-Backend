package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Company;
import com.job_Portal_Backend.job_portal_backend.entity.Job;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

        @Query("SELECT j FROM Job j WHERE " +
                        "(j.status = 'ACTIVE' OR j.status IS NULL) AND " +
                        "j.isDeleted = false AND " +
                        "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
                        "(:minSalary IS NULL OR j.minSalary >= :minSalary) AND " +
                        "(:maxSalary IS NULL OR j.maxSalary <= :maxSalary) AND " +
                        "(:jobType IS NULL OR j.jobType = :jobType) AND " +
                        "(:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) AND " +
                        "(:keyword IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        " LOWER(j.skills) LIKE LOWER(CONCAT('%', :keyword, '%')))")
        List<Job> findJobsWithFilters(@Param("location") String location,
                        @Param("minSalary") Double minSalary,
                        @Param("maxSalary") Double maxSalary,
                        @Param("jobType") String jobType,
                        @Param("experienceLevel") String experienceLevel,
                        @Param("keyword") String keyword);

        @Query("SELECT j FROM Job j WHERE j.recruiter.id = :recruiterId AND j.isDeleted = false")
        List<Job> findByRecruiterIdAndNotDeleted(@Param("recruiterId") Long recruiterId);

        @Query("SELECT COUNT(j) FROM Job j WHERE j.recruiter.id = :recruiterId AND j.isDeleted = false")
        long countByRecruiterIdAndNotDeleted(@Param("recruiterId") Long recruiterId);

        @Query("SELECT DISTINCT j.title FROM Job j WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.isDeleted = false AND LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY j.title ASC")
        List<String> findMatchingTitles(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT DISTINCT c.name FROM Company c JOIN Job j ON j.company = c WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.isDeleted = false AND c.isDeleted = false AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.name ASC")
        List<String> findMatchingCompanyNames(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT DISTINCT j.location FROM Job j WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.location IS NOT NULL AND j.location <> '' AND j.isDeleted = false ORDER BY j.location ASC")
        List<String> findDistinctLocations();

        @Query("SELECT DISTINCT j.jobType FROM Job j WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.jobType IS NOT NULL AND j.jobType <> '' AND j.isDeleted = false ORDER BY j.jobType ASC")
        List<String> findDistinctJobTypes();

        @Query("SELECT DISTINCT j.experienceLevel FROM Job j WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.experienceLevel IS NOT NULL AND j.experienceLevel <> '' AND j.isDeleted = false ORDER BY j.experienceLevel ASC")
        List<String> findDistinctExperienceLevels();

        @Query("SELECT MIN(j.minSalary) FROM Job j WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.isDeleted = false")
        Double findMinSalary();

        @Query("SELECT MAX(j.maxSalary) FROM Job j WHERE (j.status = 'ACTIVE' OR j.status IS NULL) AND j.isDeleted = false")
        Double findMaxSalary();

        List<Job> findByCompanyAndIsDeletedFalse(Company company);
}
