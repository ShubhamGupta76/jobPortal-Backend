package com.job_Portal_Backend.job_portal_backend.config;

import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.RoleRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // No default on purpose: the initial admin password must never have a hardcoded/guessable
    // fallback. Blank here means the env var was not set, checked explicitly in run() below so
    // the failure message is clear rather than an opaque placeholder-resolution error.
    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String adminInitialPassword;

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByName("USER").isEmpty()) {
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        if (roleRepository.findByName("RECRUITER").isEmpty()) {
            Role recruiterRole = new Role();
            recruiterRole.setName("RECRUITER");
            roleRepository.save(recruiterRole);
        }

        if (roleRepository.findByName("ADMIN").isEmpty()) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            roleRepository.save(adminRole);
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role was not initialized"));

        if (adminInitialPassword == null || adminInitialPassword.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD environment variable is required to seed the initial admin account");
        }

        User adminUser = userRepository.findByEmail("gshubhamkumar01@gmail.com")
                .orElseGet(User::new);

        adminUser.setEmail("gshubhamkumar01@gmail.com");
        adminUser.setPassword(passwordEncoder.encode(adminInitialPassword));
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setPhone("");
        adminUser.setRoles(Set.of(adminRole));
        adminUser.setIsEmailVerified(true);
        adminUser.setIsDeleted(false);
        adminUser.setIsBlocked(false);
        userRepository.save(adminUser);
    }
}
