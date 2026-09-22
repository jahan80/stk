
package com.starterkit.auth.auth.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final Long userId;
    private final String username;
    private final String email;
    private final String mobileNumber;
    private final String firstName;
    private final String lastName;
}

