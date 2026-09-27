package com.starterkit.gateway.ratelimit.api;

import com.starterkit.gateway.ratelimit.api.dto.ActiveRateLimitResponse;
import com.starterkit.gateway.ratelimit.api.dto.RateLimitRequest;
import com.starterkit.gateway.ratelimit.api.dto.RateLimitResponse;
import com.starterkit.gateway.ratelimit.application.RateLimitConfigService;
import com.starterkit.gateway.shared.api.response.ApiCode;
import com.starterkit.gateway.shared.api.response.ApiResponse;
import com.starterkit.gateway.shared.api.response.ApiResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Rate Limit Configuration API.
 *
 * Note: These endpoints are protected by JWT auth filter.
 * Only ADMIN role can modify.
 */
@RestController
@RequestMapping("/gateway/rate-limits")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitConfigService service;
    private final ApiResponseFactory responseFactory;

    // =========================================================
    // PUBLIC (internal) - for gateway filter (cached)
    // =========================================================

    @GetMapping("/active")
    public Mono<ApiResponse<List<ActiveRateLimitResponse>>> getActive() {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.getActive()
        ));
    }

    // =========================================================
    // ADMIN API
    // =========================================================

    @GetMapping
    public Mono<ApiResponse<List<RateLimitResponse>>> listAll() {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.listAll()
        ));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<RateLimitResponse>> getById(@PathVariable Long id) {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.getById(id)
        ));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<RateLimitResponse>> create(
            @Valid @RequestBody RateLimitRequest request
    ) {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.create(request)
        ));
    }

    @PutMapping("/{id}")
    public Mono<ApiResponse<RateLimitResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RateLimitRequest request
    ) {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.update(id, request)
        ));
    }

    @DeleteMapping("/{id}")
    public Mono<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                null
        ));
    }

    @PostMapping("/{id}/reset-default")
    public Mono<ApiResponse<RateLimitResponse>> resetToDefault(@PathVariable Long id) {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.resetToDefault(id)
        ));
    }

    @PostMapping("/reset-defaults")
    public Mono<ApiResponse<List<RateLimitResponse>>> resetAllToDefaults() {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.resetAllToDefault()
        ));
    }

    @PostMapping("/{id}/enable")
    public Mono<ApiResponse<RateLimitResponse>> enable(@PathVariable Long id) {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.setEnabled(id, true)
        ));
    }

    @PostMapping("/{id}/disable")
    public Mono<ApiResponse<RateLimitResponse>> disable(@PathVariable Long id) {
        return Mono.just(responseFactory.success(
                ApiCode.SUCCESS,
                service.setEnabled(id, false)
        ));
    }
}
