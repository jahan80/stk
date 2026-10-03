package com.starterkit.auth.shared.infrastructure.config;

import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates a default admin user on startup if not present.
 *
 * ⚠️ DEV ONLY: Must be disabled before production deployment.
 * Set app.default-admin.enabled=false in production.
 *
 * Runs on ApplicationReadyEvent (after Flyway migrations AND after
 * all beans are ready), so the ADMIN role is guaranteed to exist.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.default-admin.enabled",
        havingValue = "true"
)
public class DataInitializer {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username:admin}")
    private String username;

    @Value("${app.default-admin.password:admin123}")
    private String password;

    @Value("${app.default-admin.email:admin@starterkit.local}")
    private String email;

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdmin() {
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

        log.warn("⚠️  Default admin created: username='{}'. " +
                "CHANGE THE PASSWORD BEFORE PRODUCTION!", username);
    }
}
