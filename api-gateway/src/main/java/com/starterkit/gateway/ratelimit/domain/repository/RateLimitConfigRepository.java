package com.starterkit.gateway.ratelimit.domain.repository;

import com.starterkit.gateway.ratelimit.domain.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RateLimitConfigRepository 
        extends JpaRepository<RateLimitConfig, Long> {

    List<RateLimitConfig> findAllByEnabledTrueOrderByPriorityDesc();

    List<RateLimitConfig> findAllByOrderByPriorityDesc();
}
