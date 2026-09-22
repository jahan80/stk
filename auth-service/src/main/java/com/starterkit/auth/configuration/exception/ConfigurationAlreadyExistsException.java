package com.starterkit.auth.configuration.exception;

import lombok.Getter;

@Getter
public class ConfigurationAlreadyExistsException extends RuntimeException {

    private final String configKey;

    public ConfigurationAlreadyExistsException(String configKey) {
        super("Configuration already exists: " + configKey);
        this.configKey = configKey;
    }
}