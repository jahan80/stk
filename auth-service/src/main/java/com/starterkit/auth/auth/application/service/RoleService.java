package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.api.dto.PermissionResponse;
import com.starterkit.auth.auth.api.dto.RoleRequest;
import com.starterkit.auth.auth.api.dto.RoleResponse;
import com.starterkit.auth.auth.application.exception.PermissionNotFoundException;
import com.starterkit.auth.auth.application.exception.RoleAlreadyExistsException;
import com.starterkit.auth.auth.application.exception.RoleInUseException;
import com.starterkit.auth.auth.application.exception.RoleNotFoundException;
import com.starterkit.auth.auth.application.exception.SystemRoleProtectedException;
import com.starterkit.auth.auth.domain.entity.Permission;
import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.repository.PermissionRepository;
import com.starterkit.auth.auth.domain.repository.RoleRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public List<RoleResponse> listAll() {
        return roleRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse getById(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new RoleNotFoundException(id));
        return toResponse(role);
    }

    public List<PermissionResponse> listAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toPermissionResponse)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new RoleAlreadyExistsException(request.getName());
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setSystemRole(false);

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Transactional
    public RoleResponse update(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        if (role.isSystemRole()) {
            throw new SystemRoleProtectedException(role.getName(), "modified");
        }

        // name is immutable to avoid breaking permission references
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        if (role.isSystemRole()) {
            throw new SystemRoleProtectedException(role.getName(), "deleted");
        }

        long usersWithRole = userRepository.countByRole(role);
        if (usersWithRole > 0) {
            throw new RoleInUseException(id, usersWithRole);
        }

        role.setDeletedAt(Instant.now());
        roleRepository.save(role);
    }

    @Transactional
    public RoleResponse assignPermissions(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        Set<Permission> permissions = permissionIds.stream()
                .map(pid -> permissionRepository.findById(pid)
                        .orElseThrow(() -> new PermissionNotFoundException(pid)))
                .collect(Collectors.toSet());

        role.setPermissions(permissions);
        Role saved = roleRepository.save(role);
        return toResponse(saved);
    }

    private RoleResponse toResponse(Role role) {
        Set<PermissionResponse> perms = role.getPermissions().stream()
                .map(this::toPermissionResponse)
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .systemRole(role.isSystemRole())
                .createdAt(role.getCreatedAt())
                .permissions(perms)
                .build();
    }

    private PermissionResponse toPermissionResponse(Permission p) {
        return PermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .build();
    }
}
