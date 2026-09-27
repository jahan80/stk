package com.starterkit.gateway.shared.infrastructure.security;

import com.starterkit.gateway.shared.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
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

/**
 * Verify JWT for gateway admin endpoints (/gateway/**).
 * Other routes are NOT blocked here - they are forwarded to services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements WebFilter, Ordered {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/gateway/";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // Only verify for /gateway/** endpoints
        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            return chain.filter(exchange);
        }

        // Allow /gateway/rate-limits/active without auth (internal)
        if (path.equals("/gateway/rate-limits/active")) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);

        if (token == null) {
            return unauthorized(exchange, "Missing token");
        }

        try {
            Claims claims = jwtService.parse(token);

            if (!jwtService.isAccessToken(claims)) {
                return unauthorized(exchange, "Invalid token type");
            }

            var roles = jwtService.getRoles(claims);
            if (!roles.contains("ADMIN")) {
                return forbidden(exchange, "Admin role required");
            }

            // Add user info to headers for downstream
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", String.valueOf(jwtService.getUserId(claims)))
                    .header("X-User-Name", jwtService.getUsername(claims))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (Exception ex) {
            log.debug("JWT verification failed: {}", ex.getMessage());
            return unauthorized(exchange, "Invalid token");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
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

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }
}
