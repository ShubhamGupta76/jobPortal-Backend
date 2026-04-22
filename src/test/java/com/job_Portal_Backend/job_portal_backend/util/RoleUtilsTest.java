package com.job_Portal_Backend.job_portal_backend.util;

import com.job_Portal_Backend.job_portal_backend.entity.Role;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleUtilsTest {

    @Test
    void shouldPreferRecruiterOverUser() {
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        Role recruiterRole = new Role();
        recruiterRole.setId(2L);
        recruiterRole.setName("RECRUITER");

        String primaryRole = RoleUtils.resolvePrimaryRole(Set.of(userRole, recruiterRole));

        assertEquals("RECRUITER", primaryRole);
    }

    @Test
    void shouldPreferAdminOverRecruiter() {
        Role recruiterRole = new Role();
        recruiterRole.setId(2L);
        recruiterRole.setName("RECRUITER");

        Role adminRole = new Role();
        adminRole.setId(3L);
        adminRole.setName("ADMIN");

        String primaryRole = RoleUtils.resolvePrimaryRole(Set.of(recruiterRole, adminRole));

        assertEquals("ADMIN", primaryRole);
    }
}
