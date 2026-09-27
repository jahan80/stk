package com.starterkit.gateway.shared.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiCode {

    SUCCESS(
            "GATEWAY-000",
            "Operation completed successfully"
    ),

    RATE_LIMIT_NOT_FOUND(
            "GATEWAY-RL-001",
            "Rate limit config not found"
    ),

    RATE_LIMIT_ALREADY_EXISTS(
            "GATEWAY-RL-002",
            "Rate limit config already exists"
    ),

    RATE_LIMIT_EXCEEDED(
            "GATEWAY-RATE-001",
            "Too many requests"
    ),

    VALIDATION_ERROR(
            "GATEWAY-VALIDATION-001",
            "Request validation failed"
    ),

    UNAUTHORIZED(
            "GATEWAY-AUTH-001",
            "Unauthorized"
    ),

    FORBIDDEN(
            "GATEWAY-AUTH-002",
            "Access denied"
    ),

    INTERNAL_ERROR(
            "GATEWAY-500",
            "An unexpected error occurred"
    );

    private final String code;
    private final String defaultMessage;
}
