package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.api.dto.AssignRoleRequest;
import com.starterkit.auth.auth.api.dto.CreateUserRequest;
import com.starterkit.auth.auth.application.exception.UserAlreadyExistsException;
import com.starterkit.auth.auth.api.dto.UserDetailResponse;
import com.starterkit.auth.auth.api.dto.UserSummaryResponse;
import com.starterkit.auth.auth.application.exception.DeletedRoleException;
import com.starterkit.auth.auth.application.exception.RoleNotFoundException;
import com.starterkit.auth.auth.application.exception.UserNotFoundException;
import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserSummaryResponse> listAll() {
        return userRepository.findAllWithRole().stream()
                .map(this::toSummary)
                .toList();
    }

    public UserDetailResponse getById(Long id) {
        User user = userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return toDetail(user);
    }

    @Transactional
    public UserDetailResponse assignRole(Long userId, AssignRoleRequest request) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RoleNotFoundException(request.getRoleId()));

        if (role.getDeletedAt() != null) {
            throw new DeletedRoleException(role.getId());
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        return toDetail(saved);
    }

    @Transactional
    public UserDetailResponse setEnabled(Long userId, boolean enabled) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return toDetail(saved);
    }

    private UserSummaryResponse toSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .enabled(user.isEnabled())
                .build();
    }

    private UserDetailResponse toDetail(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("username");
        }

        if (StringUtils.hasText(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email");
        }

        if (StringUtils.hasText(request.getMobileNumber())
                && userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new UserAlreadyExistsException("mobileNumber");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new RoleNotFoundException(request.getRoleId()));

        if (role.getDeletedAt() != null) {
            throw new DeletedRoleException(role.getId());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(role);

        User saved = userRepository.save(user);
        return toDetail(saved);
    }

}
