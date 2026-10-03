package com.starterkit.gateway.ratelimit.application;

import com.starterkit.gateway.ratelimit.api.dto.ActiveRateLimitResponse;
import com.starterkit.gateway.ratelimit.api.dto.RateLimitRequest;
import com.starterkit.gateway.ratelimit.api.dto.RateLimitResponse;
import com.starterkit.gateway.ratelimit.domain.entity.RateLimitConfig;
import com.starterkit.gateway.ratelimit.domain.repository.RateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateLimitConfigService {

    private final RateLimitConfigRepository repository;

    public List<RateLimitResponse> listAll() {
        return repository.findAllByOrderByPriorityDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RateLimitResponse getById(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rate limit config not found: " + id));
    }

    @Cacheable(value = "activeRateLimits")
    public List<ActiveRateLimitResponse> getActive() {
        log.debug("Loading active rate limits from DB");
        return repository.findAllByEnabledTrueOrderByPriorityDesc()
                .stream()
                .map(this::toActiveResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "activeRateLimits", allEntries = true)
    public RateLimitResponse create(RateLimitRequest request) {
        // Pre-check for duplicate: (pathPattern, method, keyType)
        // Note: method may be null → use COALESCE-equivalent check.
        boolean duplicate = repository.findAll().stream()
                .anyMatch(c -> c.getPathPattern().equals(request.getPathPattern())
                        && java.util.Objects.equals(c.getMethod(), request.getMethod())
                        && c.getKeyType() == request.getKeyType());
        if (duplicate) {
            throw new IllegalStateException(
                    "Rate limit already exists for path=" + request.getPathPattern()
                            + ", method=" + request.getMethod()
                            + ", keyType=" + request.getKeyType());
        }

        RateLimitConfig config = new RateLimitConfig();
        config.setPathPattern(request.getPathPattern());
        config.setMethod(request.getMethod());
        config.setKeyType(request.getKeyType());
        config.setRequestsPerWindow(request.getRequestsPerWindow());
        config.setWindowSeconds(request.getWindowSeconds());
        config.setBurstCapacity(request.getBurstCapacity());
        config.setDefaultRequestsPerWindow(request.getRequestsPerWindow());
        config.setDefaultWindowSeconds(request.getWindowSeconds());
        config.setDefaultBurstCapacity(request.getBurstCapacity());
        config.setDefaultEnabled(request.isEnabled());
        config.setPriority(request.getPriority());
        config.setDescription(request.getDescription());
        config.setEnabled(request.isEnabled());

        return toResponse(repository.save(config));
    }

    @Transactional
    @CacheEvict(value = "activeRateLimits", allEntries = true)
    public RateLimitResponse update(Long id, RateLimitRequest request) {
        RateLimitConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rate limit config not found: " + id));

        config.setPathPattern(request.getPathPattern());
        config.setMethod(request.getMethod());
        config.setKeyType(request.getKeyType());
        config.setRequestsPerWindow(request.getRequestsPerWindow());
        config.setWindowSeconds(request.getWindowSeconds());
        config.setBurstCapacity(request.getBurstCapacity());
        config.setPriority(request.getPriority());
        config.setDescription(request.getDescription());
        config.setEnabled(request.isEnabled());

        return toResponse(repository.save(config));
    }

    @Transactional
    @CacheEvict(value = "activeRateLimits", allEntries = true)
    public void delete(Long id) {
        RateLimitConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rate limit config not found: " + id));
        repository.delete(config);
    }

    @Transactional
    @CacheEvict(value = "activeRateLimits", allEntries = true)
    public RateLimitResponse resetToDefault(Long id) {
        RateLimitConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rate limit config not found: " + id));

        config.setRequestsPerWindow(config.getDefaultRequestsPerWindow());
        config.setWindowSeconds(config.getDefaultWindowSeconds());
        config.setBurstCapacity(config.getDefaultBurstCapacity());
        config.setEnabled(config.isDefaultEnabled());

        return toResponse(repository.save(config));
    }

    @Transactional
    @CacheEvict(value = "activeRateLimits", allEntries = true)
    public List<RateLimitResponse> resetAllToDefault() {
        List<RateLimitConfig> configs = repository.findAll();

        configs.forEach(c -> {
            c.setRequestsPerWindow(c.getDefaultRequestsPerWindow());
            c.setWindowSeconds(c.getDefaultWindowSeconds());
            c.setBurstCapacity(c.getDefaultBurstCapacity());
            c.setEnabled(c.isDefaultEnabled());
        });

        return repository.saveAll(configs)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "activeRateLimits", allEntries = true)
    public RateLimitResponse setEnabled(Long id, boolean enabled) {
        RateLimitConfig config = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rate limit config not found: " + id));
        config.setEnabled(enabled);
        return toResponse(repository.save(config));
    }

    private RateLimitResponse toResponse(RateLimitConfig c) {
        return RateLimitResponse.builder()
                .id(c.getId())
                .pathPattern(c.getPathPattern())
                .method(c.getMethod())
                .keyType(c.getKeyType().name())
                .requestsPerWindow(c.getRequestsPerWindow())
                .windowSeconds(c.getWindowSeconds())
                .burstCapacity(c.getBurstCapacity())
                .defaultRequestsPerWindow(c.getDefaultRequestsPerWindow())
                .defaultWindowSeconds(c.getDefaultWindowSeconds())
                .defaultBurstCapacity(c.getDefaultBurstCapacity())
                .defaultEnabled(c.isDefaultEnabled())
                .enabled(c.isEnabled())
                .priority(c.getPriority())
                .description(c.getDescription())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ActiveRateLimitResponse toActiveResponse(RateLimitConfig c) {
        return ActiveRateLimitResponse.builder()
                .pathPattern(c.getPathPattern())
                .method(c.getMethod())
                .keyType(c.getKeyType().name())
                .requestsPerWindow(c.getRequestsPerWindow())
                .windowSeconds(c.getWindowSeconds())
                .burstCapacity(c.getBurstCapacity())
                .priority(c.getPriority())
                .build();
    }
}
