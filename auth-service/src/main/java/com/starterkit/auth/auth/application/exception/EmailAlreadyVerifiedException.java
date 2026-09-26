package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class EmailAlreadyVerifiedException extends RuntimeException {

    private final String email;

    public EmailAlreadyVerifiedException(String email) {
        super("Email already verified: " + email);
        this.email = email;
    }
}
