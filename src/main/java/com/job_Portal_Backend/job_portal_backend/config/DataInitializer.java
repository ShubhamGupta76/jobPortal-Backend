package com.job_Portal_Backend.job_portal_backend.config;

import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.entity.User;
import com.job_Portal_Backend.job_portal_backend.repository.RoleRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

        User adminUser = userRepository.findByEmail("gshubhamkumar01@gmail.com")
                .orElseGet(User::new);

        adminUser.setEmail("gshubhamkumar01@gmail.com");
        adminUser.setPassword(passwordEncoder.encode("7643023962"));
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
