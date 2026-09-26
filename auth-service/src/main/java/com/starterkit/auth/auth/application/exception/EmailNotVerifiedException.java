package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class EmailNotVerifiedException extends RuntimeException {

    private final String email;

    public EmailNotVerifiedException(String email) {
        super("Email not verified: " + email);
        this.email = email;
    }
}
