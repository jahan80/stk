package com.starterkit.gateway.ratelimit.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActiveRateLimitResponse {
    private final String pathPattern;
    private final String method;
    private final String keyType;
    private final int requestsPerWindow;
    private final int windowSeconds;
    private final Integer burstCapacity;
    private final int priority;
}
