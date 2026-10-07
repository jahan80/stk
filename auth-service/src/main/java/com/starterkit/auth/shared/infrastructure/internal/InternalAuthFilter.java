package com.starterkit.auth.shared.infrastructure.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Guards /internal/** endpoints with a shared token.
 *
 * Runs BEFORE Spring Security (registered via addFilterBefore in
 * SecurityConfig). Returns 401 with a JSON body on failure.
 *
 * Header: X-Internal-Token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String INTERNAL_PATH_PREFIX = "/internal/";
    public static final String TOKEN_HEADER = "X-Internal-Token";

    private final InternalApiProperties properties;

    @jakarta.annotation.PostConstruct
    void validateTokenConfig() {
        String token = properties.getToken();

        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "internal-api.token must be configured (env INTERNAL_API_TOKEN)");
        }

        if (token.length() < 32) {
            throw new IllegalStateException(
                    "internal-api.token must be at least 32 characters. " +
                    "Set INTERNAL_API_TOKEN to a strong random value.");
        }

        if ("dev-only-internal-token-change-me".equals(token)) {
            // Refuse the well-known dev default unless explicitly allowed.
            String allowed = System.getenv("ALLOW_INSECURE_INTERNAL_TOKEN");
            if (!"true".equalsIgnoreCase(allowed)) {
                throw new IllegalStateException(
                        "INTERNAL_API_TOKEN is still the well-known dev default. " +
                        "Set a strong random value, or set ALLOW_INSECURE_INTERNAL_TOKEN=true " +
                        "for local development only.");
            }
            log.warn("*** Using the well-known dev INTERNAL_API_TOKEN. " +
                     "NEVER do this in production. ***");
        }
    }


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain)
            throws ServletException, IOException {

        String provided = request.getHeader(TOKEN_HEADER);
        String expected = properties.getToken();

        if (!StringUtils.hasText(expected)) {
            log.error("INTERNAL_API_TOKEN is empty; rejecting internal call");
            writeUnauthorized(response, "Internal API not configured");
            return;
        }

        if (!expected.equals(provided)) {
            log.warn("Internal API: invalid token from {} for {}",
                    request.getRemoteAddr(), request.getRequestURI());
            writeUnauthorized(response, "Invalid internal token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"code\":\"AUTH-INTERNAL-401\","
                        + "\"message\":\"" + message + "\"}"
        );
    }
}
