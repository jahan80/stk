package com.starterkit.gateway.shared.infrastructure.security;

import com.starterkit.gateway.shared.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Global JWT gate at the gateway.
 *
 * FIX P1-4 (option A): the gateway now enforces authentication for
 * all /audit/**, /tickets/**, /notify/** and /gateway/** paths before
 * forwarding to downstream services. Downstream services still verify
 * the token themselves (defense in depth), but the client now gets a
 * proper 401 instead of an opaque 403 from audit-service.
 *
 * Public paths (no JWT required):
 *   - /auth/register, /auth/login, /auth/refresh, /auth/logout
 *   - /auth/email/**, /auth/mobile/**, /auth/password/**
 *   - /auth/configurations (GET, read-only, used by frontend)
 *   - Swagger / OpenAPI docs
 *
 * Order:
 *   IdentitySanitizingFilter (WebFilter)  = HIGHEST_PRECEDENCE
 *   JwtAuthFilter           (WebFilter)  = HIGHEST_PRECEDENCE + 10
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter implements WebFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /** Paths that require an ADMIN role. */
    private static final String ADMIN_PATH_PREFIX = "/gateway/";

    /** Paths that are completely public (no JWT required). */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/logout",
            "/auth/email/verify",
            "/auth/email/resend-verification",
            "/auth/mobile/verify",
            "/auth/mobile/resend-verification",
            "/auth/password/forgot",
            "/auth/password/reset",
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars"
    );

    /** Prefixes that are always public. */
    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars"
    );

    /**
     * GET /auth/configurations/** is public (frontend needs feature flags).
     * Other methods on that path require auth.
     */
    private static final String CONFIG_READ_PREFIX = "/auth/configurations";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "";

        // Public endpoints always pass.
        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        // All other paths require a valid JWT.
        String token = extractToken(request);

        if (token == null) {
            return unauthorized(exchange, "Missing Authorization header");
        }

        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception ex) {
            log.debug("JWT verify failed for {}: {}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }

        if (!jwtService.isAccessToken(claims)) {
            return unauthorized(exchange, "Invalid token type");
        }

        // Admin-only paths.
        if (path.startsWith(ADMIN_PATH_PREFIX)) {
            var roles = jwtService.getRoles(claims);
            if (!roles.contains("ADMIN")) {
                return forbidden(exchange, "Admin role required");
            }
        }

        // Token is valid; IdentitySanitizingFilter has already injected
        // verified X-User-Id / X-User-Name / X-User-Roles headers.
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isPublic(String path, String method) {
        // Exact matches
        for (String p : PUBLIC_PATHS) {
            if (path.equals(p)) return true;
        }
        // Prefix matches
        for (String p : PUBLIC_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        // GET /auth/configurations (feature flags for frontend)
        if ("GET".equals(method) && path.startsWith(CONFIG_READ_PREFIX)) {
            return true;
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        return writeError(exchange, HttpStatus.UNAUTHORIZED,
                "{\"success\":false,\"code\":\"GATEWAY-AUTH-001\",\"message\":\"" + reason + "\"}");
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String reason) {
        return writeError(exchange, HttpStatus.FORBIDDEN,
                "{\"success\":false,\"code\":\"GATEWAY-AUTH-002\",\"message\":\"" + reason + "\"}");
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String body) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String origin = exchange.getRequest().getHeaders().getOrigin();
        if (origin != null) {
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.getHeaders().set(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
