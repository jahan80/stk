package com.starterkit.audit.shared.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiCode implements com.starterkit.commons.web.ApiCode {

    SUCCESS(
            "AUDIT-000",
            "Operation completed successfully"
    ),

    AUDIT_EVENT_NOT_FOUND(
            "AUDIT-EVENT-001",
            "Audit event not found"
    ),

    VALIDATION_ERROR(
            "AUDIT-VALIDATION-001",
            "Request validation failed"
    ),

    FORBIDDEN(
            "AUDIT-403",
            "Access denied: insufficient permissions"
    ),

    INTERNAL_ERROR(
            "AUDIT-500",
            "An unexpected error occurred"
    );

    private final String code;
    private final String defaultMessage;
}
