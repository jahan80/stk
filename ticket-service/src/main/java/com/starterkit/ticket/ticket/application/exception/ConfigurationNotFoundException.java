package com.starterkit.ticket.ticket.application.exception;

public class ConfigurationNotFoundException extends RuntimeException {
    public ConfigurationNotFoundException(String key) {
        super("Configuration not found: " + key);
    }
}
