package com.starterkit.auth.auth.domain.repository;

import com.starterkit.auth.auth.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    Set<Permission> findByCodeIn(Set<String> codes);
}
