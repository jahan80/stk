package com.starterkit.auth.shared.infrastructure.internal;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for service-to-service internal endpoints.
 *
 * All requests to /internal/** must carry header:
 *   X-Internal-Token: <internal-api.token>
 *
 * The token is shared via env var across services.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "internal-api")
public class InternalApiProperties {

    /**
     * Shared secret for internal service-to-service calls.
     * MUST be overridden via INTERNAL_API_TOKEN env var in production.
     * Default is intentionally weak so it's obvious in dev.
     */
    private String token = "dev-only-internal-token-change-me";
}
