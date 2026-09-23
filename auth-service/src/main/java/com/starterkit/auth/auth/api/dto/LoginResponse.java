package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserInfo user;

    @Getter
    @Builder
    public static class UserInfo {
        private final Long id;
        private final String username;
        private final String email;
        private final String role;
    }
}
