package com.starterkit.gateway.ratelimit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RateLimitResponse {
    private Long id;
    private String pathPattern;
    private String method;
    private String keyType;
    private int requestsPerWindow;
    private int windowSeconds;
    private Integer burstCapacity;
    private int defaultRequestsPerWindow;
    private int defaultWindowSeconds;
    private Integer defaultBurstCapacity;
    private boolean defaultEnabled;
    private boolean enabled;
    private int priority;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
