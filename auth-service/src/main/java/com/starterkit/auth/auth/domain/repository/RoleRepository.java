package com.starterkit.auth.auth.domain.repository;

import com.starterkit.auth.auth.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    Optional<Role> findByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByName(String name);

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    @Query("SELECT r FROM Role r WHERE r.deletedAt IS NULL ORDER BY r.name")
    List<Role> findAllActive();
}
