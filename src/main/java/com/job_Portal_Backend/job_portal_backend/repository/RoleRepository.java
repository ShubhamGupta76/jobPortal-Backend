package com.job_Portal_Backend.job_portal_backend.repository;

import com.job_Portal_Backend.job_portal_backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}