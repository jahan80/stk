package com.starterkit.gateway.ratelimit.infrastructure.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.starterkit.gateway.ratelimit.api.dto.ActiveRateLimitResponse;
import com.starterkit.gateway.ratelimit.application.RateLimitConfigService;
import com.starterkit.gateway.ratelimit.config.RateLimitProperties;
import com.starterkit.gateway.ratelimit.infrastructure.bucket.BucketRegistry;
import com.starterkit.gateway.ratelimit.infrastructure.bucket.TokenBucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RateLimitConfigService configService;
    private final BucketRegistry bucketRegistry;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().name();

        // Find matching config
        MatchResult result = findMatchingConfig(path, method);

        // Case 1: config load failed (DB down, cache error, ...)
        if (result.error()) {
            if (properties.isFailOpen()) {
                log.warn("Rate limit: config load failed, fail-OPEN → allowing {} {}",
                        method, path);
                return chain.filter(exchange);
            }
            log.error("Rate limit: config load failed, fail-CLOSED → rejecting {} {}",
                    method, path);
            return serviceUnavailable(exchange);
        }

        // Case 2: no matching config
        if (!result.found() || !properties.getDefaultLimit().isEnabled()) {
            return chain.filter(exchange);
        }

        ActiveRateLimitResponse config = result.config();
        String key = buildKey(exchange, config);

        // Get or create bucket
        long capacity = config.getBurstCapacity() != null
                ? config.getBurstCapacity()
                : config.getRequestsPerWindow();

        long refillRate = Math.max(1,
                config.getRequestsPerWindow() / Math.max(1, config.getWindowSeconds()));

        TokenBucket bucket = bucketRegistry.getOrCreate(key, capacity, refillRate);

        if (!bucket.tryConsume()) {
            return tooManyRequests(exchange, config, bucket);
        }

        // Add rate limit headers
        if (properties.isIncludeHeaders()) {
            addHeaders(exchange, config, bucket);
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    // =====================================================
    // Matching
    // =====================================================

    /**
     * Result of looking up a matching rate-limit config.
     */
    private record MatchResult(boolean found, ActiveRateLimitResponse config, boolean error) {}

    private MatchResult findMatchingConfig(String path, String method) {
        try {
            List<ActiveRateLimitResponse> configs = configService.getActive();

            Optional<ActiveRateLimitResponse> match = configs.stream()
                    .filter(c -> matches(c, path, method))
                    .findFirst();

            return new MatchResult(match.isPresent(), match.orElse(null), false);

        } catch (Exception ex) {
            log.error("Failed to load rate limit configs", ex);
            return new MatchResult(false, null, true);
        }
    }

    private boolean matches(ActiveRateLimitResponse config, String path, String method) {
        if (!PATH_MATCHER.match(config.getPathPattern(), path)) {
            return false;
        }
        if (config.getMethod() != null && !config.getMethod().isBlank()) {
            return config.getMethod().equalsIgnoreCase(method);
        }
        return true;
    }

    // =====================================================
    // Key Building
    // =====================================================

    private String buildKey(ServerWebExchange exchange, ActiveRateLimitResponse config) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(request);
        String path = request.getPath().value();

        return switch (config.getKeyType()) {
            case "IP" -> clientIp;
            case "IP_PATH" -> clientIp + ":" + path;
            case "USER" -> {
                String userId = request.getHeaders().getFirst("X-User-Id");
                yield userId != null ? "user:" + userId : clientIp;
            }
            case "USER_PATH" -> {
                String userId = request.getHeaders().getFirst("X-User-Id");
                yield (userId != null ? "user:" + userId : clientIp) + ":" + path;
            }
            default -> clientIp + ":" + path;
        };
    }

    /**
     * Resolve client IP for rate-limit key.
     *
     * SECURITY:
     *   - If trustProxy=false (default), we IGNORE X-Forwarded-For and
     *     use the raw TCP peer address. This prevents spoofing.
     *   - If trustProxy=true, we trust the first XFF entry.
     *     Only enable when a trusted proxy overwrites XFF upstream.
     */
    private String getClientIp(ServerHttpRequest request) {
        if (properties.isTrustProxy()) {
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // First IP in the list = original client (per RFC 7239)
                return xForwardedFor.split(",")[0].trim();
            }
        }

        if (request.getRemoteAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    // =====================================================
    // Headers
    // =====================================================

    private void addHeaders(ServerWebExchange exchange, ActiveRateLimitResponse config, TokenBucket bucket) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-RateLimit-Limit",
                String.valueOf(config.getRequestsPerWindow()));
        response.getHeaders().add("X-RateLimit-Remaining",
                String.valueOf(bucket.getAvailableTokens()));
        response.getHeaders().add("X-RateLimit-Reset",
                String.valueOf(Instant.now().plusSeconds(config.getWindowSeconds()).getEpochSecond()));
    }

    // =====================================================
    // 503 Response (fail-closed)
    // =====================================================

    private Mono<Void> serviceUnavailable(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "success", false,
                "code", "GATEWAY-RATE-002",
                "message", "Rate limit unavailable (fail-closed mode)",
                "data", Map.of(
                        "path", exchange.getRequest().getPath().value()
                ),
                "timestamp", Instant.now().toString()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            log.error("Failed to write 503 response", ex);
            return response.setComplete();
        }
    }

    // =====================================================
    // 429 Response
    // =====================================================

    private Mono<Void> tooManyRequests(
            ServerWebExchange exchange,
            ActiveRateLimitResponse config,
            TokenBucket bucket
    ) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        long retryAfter = bucket.getSecondsUntilNextToken();

        response.getHeaders().add("Retry-After", String.valueOf(retryAfter));
        response.getHeaders().add("X-RateLimit-Limit",
                String.valueOf(config.getRequestsPerWindow()));
        response.getHeaders().add("X-RateLimit-Remaining", "0");
        response.getHeaders().add("X-RateLimit-Reset",
                String.valueOf(Instant.now().plusSeconds(retryAfter).getEpochSecond()));

        Map<String, Object> body = Map.of(
                "success", false,
                "code", "GATEWAY-RATE-001",
                "message", "Too many requests",
                "data", Map.of(
                        "path", exchange.getRequest().getPath().value(),
                        "limit", config.getRequestsPerWindow(),
                        "window", config.getWindowSeconds(),
                        "retryAfter", retryAfter
                ),
                "timestamp", Instant.now().toString()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            log.error("Failed to write 429 response", ex);
            return response.setComplete();
        }
    }
}
