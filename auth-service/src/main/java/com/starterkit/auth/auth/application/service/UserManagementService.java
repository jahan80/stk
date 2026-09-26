package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.api.dto.AssignRoleRequest;
import com.starterkit.auth.auth.api.dto.UserDetailResponse;
import com.starterkit.auth.auth.api.dto.UserSummaryResponse;
import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public List<UserSummaryResponse> listAll() {
        return userRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    public UserDetailResponse getById(Long id) {
        User user = userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + id));
        return toDetail(user);
    }

    @Transactional
    public UserDetailResponse assignRole(Long userId, AssignRoleRequest request) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + userId));

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Role not found: " + request.getRoleId()));

        if (role.getDeletedAt() != null) {
            throw new IllegalStateException("Cannot assign deleted role");
        }

        user.setRole(role);
        User saved = userRepository.save(user);
        return toDetail(saved);
    }

    @Transactional
    public UserDetailResponse setEnabled(Long userId, boolean enabled) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "User not found: " + userId));

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
}
