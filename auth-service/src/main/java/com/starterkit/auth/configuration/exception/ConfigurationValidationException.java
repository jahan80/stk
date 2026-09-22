package com.starterkit.auth.configuration.exception;

import lombok.Getter;

@Getter
public class ConfigurationValidationException extends RuntimeException {

    public enum Reason {
        REQUIRED,
        DISABLED,
        INVALID_VALUE
    }

    private final String field;
    private final Reason reason;

    public ConfigurationValidationException(
            String field,
            Reason reason
    ) {
        super(buildMessage(field, reason));
        this.field = field;
        this.reason = reason;
    }

    private static String buildMessage(
            String field,
            Reason reason
    ) {
        return switch (reason) {
            case REQUIRED ->
                    "Field is required: " + field;

            case DISABLED ->
                    "Field is disabled: " + field;

            case INVALID_VALUE ->
                    "Invalid value for field: " + field;
        };
    }
}