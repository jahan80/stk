package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.application.exception.DeletedRoleException;
import com.starterkit.auth.auth.application.exception.RoleNotFoundException;
import com.starterkit.auth.auth.application.exception.UserAlreadyExistsException;
import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Single source of truth for user creation.
 *
 * Used by:
 * - AuthService.register      (self-registration, role = USER)
 * - UserManagementService     (admin creates user, custom role)
 */
@Service
@RequiredArgsConstructor
public class UserCreationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserCreationData data) {

        // Check duplicates
        if (userRepository.existsByUsername(data.username())) {
            throw new UserAlreadyExistsException("username");
        }

        if (StringUtils.hasText(data.email())
                && userRepository.existsByEmail(data.email())) {
            throw new UserAlreadyExistsException("email");
        }

        if (StringUtils.hasText(data.mobileNumber())
                && userRepository.existsByMobileNumber(data.mobileNumber())) {
            throw new UserAlreadyExistsException("mobileNumber");
        }

        // Resolve role (null-safe)
        if (data.roleId() == null) {
            throw new RoleNotFoundException(null);
        }
        Role role = roleRepository.findById(data.roleId())
                .orElseThrow(() -> new RoleNotFoundException(data.roleId()));

        if (role.getDeletedAt() != null) {
            throw new DeletedRoleException(role.getId());
        }

        // Create user
        User user = new User();
        user.setUsername(data.username());
        user.setPassword(passwordEncoder.encode(data.password()));
        user.setEmail(data.email());
        user.setMobileNumber(data.mobileNumber());
        user.setFirstName(data.firstName());
        user.setLastName(data.lastName());
        user.setRole(role);

        return userRepository.save(user);
    }

    public record UserCreationData(
            String username,
            String password,
            String email,
            String mobileNumber,
            String firstName,
            String lastName,
            Long roleId
    ) {}
}
