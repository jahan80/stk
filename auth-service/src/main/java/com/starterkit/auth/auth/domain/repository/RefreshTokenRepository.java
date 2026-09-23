package com.starterkit.auth.auth.domain.repository;

import com.starterkit.auth.auth.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByJti(String jti);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke all active tokens for a user (e.g. on password change or logout all devices)
     */
    @Modifying
    @Query("""
            UPDATE RefreshToken rt
            SET rt.revoked = true, rt.revokedAt = :now
            WHERE rt.user.id = :userId AND rt.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    /**
     * Delete expired tokens (scheduled cleanup)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}