package com.starterkit.notif.shared.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiCode implements com.starterkit.commons.web.ApiCode {

    SUCCESS(
            "NOTIF-000",
            "Operation completed successfully"
    ),

    NOTIFICATION_NOT_FOUND(
            "NOTIF-001",
            "Notification not found"
    ),

    SEND_FAILED(
            "NOTIF-002",
            "Failed to send notification"
    ),

    VALIDATION_ERROR(
            "NOTIF-VALIDATION-001",
            "Request validation failed"
    ),

    INTERNAL_ERROR(
            "NOTIF-500",
            "An unexpected error occurred"
    );

    private final String code;
    private final String defaultMessage;
}
