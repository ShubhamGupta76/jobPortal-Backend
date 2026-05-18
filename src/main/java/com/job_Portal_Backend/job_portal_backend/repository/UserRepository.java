package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    Optional<User> findByEmailAndIsEmailVerifiedTrueAndIsDeletedFalse(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findAllNotDeleted();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName ORDER BY u.createdAt DESC")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isDeleted = false ORDER BY u.createdAt DESC")
    List<User> findByRoleNameAndNotDeleted(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u WHERE u.isDeleted = false")
    long countNotDeleted();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isDeleted = false")
    long countByRoleNameAndNotDeleted(@Param("roleName") String roleName);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.isDeleted = false AND u.isBlocked = true")
    long countByRoleNameAndBlocked(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.isDeleted = false")
    Optional<User> findByIdAndNotDeleted(@Param("id") Long id);
}
