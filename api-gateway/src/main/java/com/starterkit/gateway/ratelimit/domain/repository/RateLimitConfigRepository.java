package com.starterkit.gateway.ratelimit.domain.repository;

import com.starterkit.gateway.ratelimit.domain.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RateLimitConfigRepository 
        extends JpaRepository<RateLimitConfig, Long> {

    List<RateLimitConfig> findAllByEnabledTrueOrderByPriorityDesc();

    List<RateLimitConfig> findAllByOrderByPriorityDesc();

    /**
     * Duplicate check that mirrors the DB unique constraint:
     *   UNIQUE (path_pattern, COALESCE(method, ''), key_type)
     *
     * Uses a native query because JPQL does not support COALESCE
     * in the WHERE clause the same way.
     */
    @Query(value = """
        SELECT EXISTS(
            SELECT 1 FROM gateway.rate_limits
            WHERE path_pattern = :pathPattern
              AND COALESCE(method, '') = COALESCE(:method, '')
              AND key_type = :keyType
              AND id <> COALESCE(:excludeId, -1)
        )
        """, nativeQuery = true)
    boolean existsDuplicate(
            @Param("pathPattern") String pathPattern,
            @Param("method") String method,
            @Param("keyType") String keyType,
            @Param("excludeId") Long excludeId
    );
}
