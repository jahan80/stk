package com.starterkit.auth.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @Size(max = 100)
    private String username;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String mobileNumber;

    @Size(min = 8, max = 255)
    private String password;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;
}