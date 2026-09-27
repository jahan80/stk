package com.starterkit.auth.auth.domain.repository;

import com.starterkit.auth.auth.domain.entity.MobileVerificationToken;
import com.starterkit.auth.auth.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MobileVerificationTokenRepository 
        extends JpaRepository<MobileVerificationToken, Long> {

    Optional<MobileVerificationToken> findByTokenHash(String tokenHash);

    Optional<MobileVerificationToken> findTopByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("DELETE FROM MobileVerificationToken t WHERE t.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM MobileVerificationToken t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
