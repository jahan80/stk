package com.starterkit.gateway.ratelimit.api.dto;

import com.starterkit.gateway.ratelimit.domain.entity.RateLimitConfig;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateLimitRequest {

    @NotBlank
    @Size(max = 200)
    private String pathPattern;

    @Size(max = 10)
    private String method;

    @NotNull
    private RateLimitConfig.KeyType keyType;

    @Min(1)
    private int requestsPerWindow;

    @Min(1)
    private int windowSeconds;

    @Min(1)
    private Integer burstCapacity;

    @Min(0)
    private int priority = 0;

    @Size(max = 255)
    private String description;

    private boolean enabled = true;
}
