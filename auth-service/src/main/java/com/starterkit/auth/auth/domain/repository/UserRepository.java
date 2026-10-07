package com.starterkit.auth.auth.domain.repository;

import com.starterkit.auth.auth.domain.entity.Role;
import com.starterkit.auth.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobileNumber);

    long countByRole(Role role);

    /**
     * Batch lookup by username (case-sensitive, exact match).
     * Used by service-to-service mention resolution.
     */
    List<User> findByUsernameIn(java.util.Collection<String> usernames);

    /**
     * All users with a given role name (used by ticket-service to
     * discover admins for on-create notifications).
     */
    @Query("SELECT u FROM User u JOIN FETCH u.role r WHERE r.name = :roleName")
    List<User> findAllByRoleName(@Param("roleName") String roleName);



    @Query("SELECT u FROM User u JOIN FETCH u.role ORDER BY u.id")
    List<User> findAllWithRole();

    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.username = :username")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions " +
           "WHERE u.mobileNumber = :mobileNumber")
    Optional<User> findByMobileNumberWithRole(@Param("mobileNumber") String mobileNumber);
}
