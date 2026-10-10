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
 * Public paths (no JWT required):
 *   - /auth/register, /auth/login, /auth/refresh, /auth/logout
 *   - /auth/email/**, /auth/mobile/**, /auth/password/**
 *   - GET /auth/configurations (read-only, used by frontend)
 *   - GET /tickets/categories (public category dropdown)
 *   - Swagger / OpenAPI docs
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtAuthFilter implements WebFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final String ADMIN_PATH_PREFIX = "/gateway/";

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

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/webjars"
    );

    private static final String CONFIG_READ_PREFIX = "/auth/configurations";
    private static final String TICKETS_CATEGORIES_PREFIX = "/tickets/categories";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod() != null ? request.getMethod().name() : "";

        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

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

        if (path.startsWith(ADMIN_PATH_PREFIX)) {
            var roles = jwtService.getRoles(claims);
            if (!roles.contains("ADMIN")) {
                return forbidden(exchange, "Admin role required");
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isPublic(String path, String method) {
        for (String p : PUBLIC_PATHS) {
            if (path.equals(p)) return true;
        }
        for (String p : PUBLIC_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        // GET /auth/configurations (feature flags for frontend)
        if ("GET".equals(method) && path.startsWith(CONFIG_READ_PREFIX)) {
            return true;
        }
        // GET /tickets/categories (category dropdown)
        if ("GET".equals(method) && path.startsWith(TICKETS_CATEGORIES_PREFIX)) {
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
