package com.starterkit.auth.configuration.exception;

import lombok.Getter;

@Getter
public class ConfigurationNotFoundException extends RuntimeException {

    private final String configKey;

    public ConfigurationNotFoundException(String configKey) {
        super("Configuration not found or disabled: " + configKey);
        this.configKey = configKey;
    }
}