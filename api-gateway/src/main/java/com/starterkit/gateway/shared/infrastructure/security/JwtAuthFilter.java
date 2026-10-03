package com.starterkit.gateway.shared.infrastructure.security;

import com.starterkit.gateway.shared.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
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

/**
 * Verifies JWT for /gateway/** endpoints.
 *
 * IMPORTANT: This filter MUST run BEFORE RateLimitingFilter because:
 *   - RateLimitingFilter uses X-User-Id for USER/USER_PATH key types.
 *   - X-User-Id is only set by this filter (trusted), never by the client.
 *
 * Order:
 *   JwtAuthFilter        = HIGHEST_PRECEDENCE + 10   (this)
 *   RateLimitingFilter   = HIGHEST_PRECEDENCE + 20
 *   LoggingFilter        = HIGHEST_PRECEDENCE
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

        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
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
        // Must run BEFORE RateLimitingFilter (HIGHEST + 20).
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
