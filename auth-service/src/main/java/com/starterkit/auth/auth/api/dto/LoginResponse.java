package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;

    private final Long userId;
    private final String username;
    private final String email;
    private final String role;
    private final boolean emailVerified;
    private final boolean mobileVerified;
    private final List<String> permissions;
}
