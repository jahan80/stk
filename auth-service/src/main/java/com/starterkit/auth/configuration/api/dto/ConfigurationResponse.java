package com.starterkit.auth.configuration.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ConfigurationResponse {

    private Long id;
    private String configKey;
    private String configValue;
    private String defaultValue;
    private String valueType;
    private String description;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}