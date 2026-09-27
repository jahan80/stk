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

    @Getter
    @Setter
    public static class Defaults {
        private int requestsPerWindow = 100;
        private int windowSeconds = 60;
        private boolean enabled = true;
    }
}
