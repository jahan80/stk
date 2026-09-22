package com.starterkit.auth.configuration.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfigurationRequest {

    @NotBlank
    @Size(max = 200)
    private String configKey;

    @NotBlank
    private String configValue;

    @NotBlank
    private String defaultValue;

    @NotBlank
    @Size(max = 20)
    private String valueType;

    @Size(max = 500)
    private String description;

    private boolean enabled = true;
}