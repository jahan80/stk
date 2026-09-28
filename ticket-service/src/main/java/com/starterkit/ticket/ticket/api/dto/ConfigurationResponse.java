package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ConfigurationResponse {
    private final Long id;
    private final String configKey;
    private final String configValue;
    private final String defaultValue;
    private final String valueType;
    private final String description;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;
}
