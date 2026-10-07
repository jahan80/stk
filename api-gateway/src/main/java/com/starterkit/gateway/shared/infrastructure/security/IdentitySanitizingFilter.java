package com.starterkit.gateway.shared.infrastructure.security;

import com.starterkit.gateway.shared.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * FIX P1-2 — Strips identity headers from client requests and, if a
 * valid access token is present, re-injects *verified* X-User-Id /
 * X-User-Name / X-User-Roles for downstream services and the rate limiter.
 *
 * Rationale:
 *   X-User-Id is trusted by RateLimitingFilter (USER / USER_PATH) and
 *   potentially by downstream services. If a client could set it
 *   directly, they could bypass user-scoped rate limits and impersonate
 *   other users in audit logs.
 *
 * Ordering (WebFilter runs BEFORE GlobalFilter in Spring Cloud Gateway):
 *   IdentitySanitizingFilter (WebFilter)        = HIGHEST_PRECEDENCE
 *   JwtAuthFilter           (WebFilter)        = HIGHEST_PRECEDENCE + 10
 *   LoggingFilter           (GlobalFilter)     = HIGHEST_PRECEDENCE
 *   RateLimitingFilter      (GlobalFilter)     = HIGHEST_PRECEDENCE + 20
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentitySanitizingFilter implements WebFilter {

    private static final String HDR_USER_ID   = "X-User-Id";
    private static final String HDR_USER_NAME = "X-User-Name";
    private static final String HDR_ROLES     = "X-User-Roles";

    private static final String AUTH_HEADER   = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String token = extractBearer(request);

        Long verifiedUserId = null;
        String verifiedUsername = null;
        List<String> verifiedRoles = List.of();

        if (token != null) {
            try {
                Claims claims = jwtService.parse(token);
                if (jwtService.isAccessToken(claims)) {
                    verifiedUserId = jwtService.getUserId(claims);
                    verifiedUsername = jwtService.getUsername(claims);
                    verifiedRoles = jwtService.getRoles(claims);
                }
            } catch (Exception ex) {
                // Invalid/expired token: do NOT reject here. Downstream
                // services decide. We only ensure a forged X-User-Id
                // cannot slip through.
                log.debug("IdentitySanitizingFilter: token rejected: {}", ex.getMessage());
            }
        }

        final Long finalUserId = verifiedUserId;
        final String finalUsername = verifiedUsername;
        final List<String> finalRoles = verifiedRoles;

        ServerHttpRequest sanitized = request.mutate()
                .headers(h -> {
                    // Always remove client-supplied identity headers.
                    h.remove(HDR_USER_ID);
                    h.remove(HDR_USER_NAME);
                    h.remove(HDR_ROLES);

                    if (finalUserId != null) {
                        h.set(HDR_USER_ID, String.valueOf(finalUserId));
                    }
                    if (finalUsername != null) {
                        h.set(HDR_USER_NAME, finalUsername);
                    }
                    if (!finalRoles.isEmpty()) {
                        h.set(HDR_ROLES, String.join(",", finalRoles));
                    }
                })
                .build();

        return chain.filter(exchange.mutate().request(sanitized).build());
    }

    private String extractBearer(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
