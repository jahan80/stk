package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final String mobileNumber;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final boolean emailVerified;
    private final boolean mobileVerified;
}
