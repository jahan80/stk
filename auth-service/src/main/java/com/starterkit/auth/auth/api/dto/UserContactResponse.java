package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Minimal contact info for service-to-service calls.
 *
 * Used by other services (ticket-service) to fetch the contact
 * channels of a user for external notifications (email/sms).
 *
 * Endpoint: GET /internal/users/{id}/contact
 */
@Getter
@Builder
public class UserContactResponse {
    private final Long id;
    private final String email;
    private final String mobileNumber;
    private final String firstName;
    private final String lastName;
}
