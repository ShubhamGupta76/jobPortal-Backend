package com.job_Portal_Backend.job_portal_backend.config;

import com.job_Portal_Backend.job_portal_backend.entity.Role;
import com.job_Portal_Backend.job_portal_backend.repository.RoleRepository;
import com.job_Portal_Backend.job_portal_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The initial admin account must never be seeded with a hardcoded/guessable password — see
 * info(2).md's production-readiness audit. ADMIN_INITIAL_PASSWORD has no default anywhere in
 * configuration; this class locks in that the seeder refuses to run without it, and never
 * encodes/saves a fallback value in its place.
 */
class DataInitializerTest {

    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final DataInitializer dataInitializer = new DataInitializer();

    @BeforeEach
    void wireMocks() {
        ReflectionTestUtils.setField(dataInitializer, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(dataInitializer, "userRepository", userRepository);
        ReflectionTestUtils.setField(dataInitializer, "passwordEncoder", passwordEncoder);

        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role()));
        when(roleRepository.findByName("RECRUITER")).thenReturn(Optional.of(new Role()));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
    }

    @Test
    void missingAdminInitialPasswordFailsFastAndNeverSavesTheAdminUser() {
        ReflectionTestUtils.setField(dataInitializer, "adminInitialPassword", "");

        assertThrows(IllegalStateException.class, () -> dataInitializer.run());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void nullAdminInitialPasswordFailsFast() {
        ReflectionTestUtils.setField(dataInitializer, "adminInitialPassword", null);

        assertThrows(IllegalStateException.class, () -> dataInitializer.run());

        verify(userRepository, never()).save(any());
    }

    @Test
    void presentAdminInitialPasswordSeedsTheAdminUserWithTheEncodedEnvValue() throws Exception {
        ReflectionTestUtils.setField(dataInitializer, "adminInitialPassword", "a-real-secret-from-env");
        when(userRepository.findByEmail("gshubhamkumar01@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("a-real-secret-from-env")).thenReturn("encoded-hash");

        dataInitializer.run();

        verify(userRepository).save(argThat(user -> "encoded-hash".equals(user.getPassword())
                && "gshubhamkumar01@gmail.com".equals(user.getEmail())));
        verify(passwordEncoder, never()).encode("7643023962");
    }
}
