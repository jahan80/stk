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
 * ⚠️ DEV ONLY. Production MUST set:
 *     APP_DEFAULT_ADMIN_ENABLED=false
 *
 * Safety guards:
 *   1. Disabled by default (enabled=${...:false}).
 *   2. If enabled=true, the password MUST be explicitly set to a
 *      strong value (>= 12 chars). Otherwise startup FAILS.
 *   3. The role lookup respects soft-delete.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "app.default-admin.enabled",
        havingValue = "true"
)
public class DataInitializer {

    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.default-admin.username:}")
    private String username;

    @Value("${app.default-admin.password:}")
    private String password;

    @Value("${app.default-admin.email:}")
    private String email;

    @Order(100)
    @EventListener(ApplicationReadyEvent.class)
    public void createDefaultAdmin() {
        // ---- Safety guards ----
        if (username == null || username.isBlank()) {
            throw new IllegalStateException(
                    "app.default-admin.enabled=true but username is empty. " +
                    "Set APP_DEFAULT_ADMIN_USERNAME or disable default admin.");
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "app.default-admin.enabled=true but password is missing or too short " +
                    "(min " + MIN_PASSWORD_LENGTH + " chars). " +
                    "Set APP_DEFAULT_ADMIN_PASSWORD to a strong value or disable default admin.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "app.default-admin.enabled=true but email is empty. " +
                    "Set APP_DEFAULT_ADMIN_EMAIL or disable default admin.");
        }

        if (userRepository.existsByUsername(username)) {
            log.info("Default admin '{}' already exists. Skipping.", username);
            return;
        }

        Role adminRole = roleRepository.findByNameAndDeletedAtIsNull("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found or soft-deleted. Did Flyway migrations run?"));

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(adminRole);
        admin.setEnabled(true);
        admin.setEmailVerified(true);  // bootstrap admin is trusted

        userRepository.save(admin);

        log.warn("⚠️  Default admin created: username='{}' (DEV ONLY). " +
                "Disable app.default-admin.enabled in production.", username);
    }
}
