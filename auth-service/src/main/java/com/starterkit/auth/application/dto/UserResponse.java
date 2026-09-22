package com.starterkit.auth.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private String mobileNumber;
    private String firstName;
    private String lastName;
}