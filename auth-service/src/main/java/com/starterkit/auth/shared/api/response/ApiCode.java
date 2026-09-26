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

    USER_NOT_FOUND(
            "AUTH-USER-005",
            "User not found"
    ),

    // ========================================
    // Role
    // ========================================
    ROLE_NOT_FOUND(
            "AUTH-ROLE-001",
            "Role not found"
    ),

    ROLE_ALREADY_EXISTS(
            "AUTH-ROLE-002",
            "Role already exists"
    ),

    ROLE_SYSTEM_PROTECTED(
            "AUTH-ROLE-003",
            "System roles cannot be modified or deleted"
    ),

    ROLE_IN_USE(
            "AUTH-ROLE-004",
            "Role is assigned to users and cannot be deleted"
    ),

    ROLE_DELETED(
            "AUTH-ROLE-005",
            "Role has been deleted and cannot be used"
    ),

    PERMISSION_NOT_FOUND(
            "AUTH-PERM-001",
            "Permission not found"
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

    FORBIDDEN(
            "AUTH-FORBIDDEN",
            "Access denied: insufficient permissions"
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