package com.starterkit.gateway.ratelimit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "gateway.rate-limit")
public class RateLimitProperties {

    private Defaults defaultLimit = new Defaults();
    private long cacheTtlSeconds = 60;
    private boolean includeHeaders = true;

    /**
     * Trust X-Forwarded-For header from upstream proxies.
     *
     * SECURITY:
     *   - false (default): use the raw TCP peer address.
     *     Safe when gateway is internet-facing (clients can spoof XFF).
     *   - true: trust the first XFF entry.
     *     Only enable when a trusted proxy (LB / Nginx / Cloudflare)
     *     is KNOWN to sit in front of the gateway and overwrite XFF.
     *
     * Mis-configuring this (true without a trusted proxy) lets any
     * client spoof its IP and bypass rate limits.
     */
    private boolean trustProxy = false;

    /**
     * Behavior when the rate-limit config cannot be loaded (DB down,
     * cache failure, etc.).
     *
     *   true  (default): fail-OPEN  — allow the request.
     *          Prioritizes availability; a broken DB does not take
     *          down the gateway. Suitable for most endpoints.
     *
     *   false: fail-CLOSED — reject with 503.
     *          Prioritizes security; suitable for sensitive endpoints
     *          (login, password reset) during an incident.
     *
     * NOTE: The final choice must be conscious. Default is open.
     */
    private boolean failOpen = true;

    @Getter
    @Setter
    public static class Defaults {
        private int requestsPerWindow = 100;
        private int windowSeconds = 60;
        private boolean enabled = true;
    }
}
