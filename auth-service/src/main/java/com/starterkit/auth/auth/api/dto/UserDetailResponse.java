package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserDetailResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final String mobileNumber;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final boolean enabled;
    private final Instant createdAt;
    private final Instant updatedAt;
}
