package com.starterkit.auth.shared.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiCode {

    // ========================================
    // Success
    // ========================================
    SUCCESS(
            "AUTH-000",
            "Operation completed successfully"
    ),

    // ========================================
    // User
    // ========================================
    USER_REGISTERED(
            "AUTH-USER-001",
            "User registered successfully"
    ),

    USERNAME_ALREADY_EXISTS(
            "AUTH-USER-002",
            "Username already exists"
    ),

    EMAIL_ALREADY_EXISTS(
            "AUTH-USER-003",
            "Email already exists"
    ),

    MOBILE_ALREADY_EXISTS(
            "AUTH-USER-004",
            "Mobile number already exists"
    ),

    USER_FIELD_UNKNOWN(
            "AUTH-USER-999",
            "Unknown user field"
    ),

    // ========================================
    // Login
    // ========================================
    LOGIN_DISABLED(
            "AUTH-LOGIN-001",
            "Login is disabled"
    ),

    INVALID_CREDENTIALS(
            "AUTH-LOGIN-002",
            "Invalid credentials"
    ),

    LOGIN_IDENTIFIER_NOT_ALLOWED(
            "AUTH-LOGIN-003",
            "This login identifier type is not allowed"
    ),

    USER_DISABLED(
            "AUTH-LOGIN-004",
            "User account is disabled"
    ),

    // ========================================
    // Configuration
    // ========================================
    CONFIGURATION_NOT_FOUND(
            "AUTH-CONFIG-001",
            "Configuration not found"
    ),

    CONFIGURATION_ALREADY_EXISTS(
            "AUTH-CONFIG-002",
            "Configuration already exists"
    ),

    // ========================================
    // Validation
    // ========================================
    VALIDATION_ERROR(
            "AUTH-VALIDATION-001",
            "Request validation failed"
    ),

    INVALID_REQUEST(
            "AUTH-VALIDATION-002",
            "Invalid request"
    ),

    METHOD_NOT_ALLOWED(
            "AUTH-VALIDATION-003",
            "HTTP method not supported for this endpoint"
    ),

    RESOURCE_NOT_FOUND(
            "AUTH-VALIDATION-004",
            "Requested resource was not found"
    ),

    UNSUPPORTED_MEDIA_TYPE(
            "AUTH-VALIDATION-005",
            "Unsupported media type"
    ),

    // ========================================
    // Server / Generic
    // ========================================
    INTERNAL_ERROR(
            "AUTH-500",
            "An unexpected error occurred"
    );

    private final String code;
    private final String defaultMessage;
}