package com.starterkit.auth.shared.infrastructure.config;

import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates a default admin user on startup if not present.
 *
 * ⚠️ DEV ONLY: Must be disabled before production deployment.
 * Set app.default-admin.enabled=false in production.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.enabled:false}")
    private boolean enabled;

    @Value("${app.default-admin.username:admin}")
    private String username;

    @Value("${app.default-admin.password:admin123}")
    private String password;

    @Value("${app.default-admin.email:admin@starterkit.local}")
    private String email;

    @Bean
    public ApplicationRunner defaultAdminInitializer() {
        return args -> {
            if (!enabled) {
                log.info("Default admin initialization is DISABLED");
                return;
            }

            if (userRepository.existsByUsername(username)) {
                log.info("Default admin '{}' already exists. Skipping.", username);
                return;
            }

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                            "ADMIN role not found. Did Flyway migrations run?"));

            User admin = new User();
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setRole(adminRole);
            admin.setEnabled(true);

            userRepository.save(admin);

            log.warn("⚠️  Default admin created: username='{}', password='{}'. " +
                     "CHANGE THIS BEFORE PRODUCTION!", username, password);
        };
    }
}
