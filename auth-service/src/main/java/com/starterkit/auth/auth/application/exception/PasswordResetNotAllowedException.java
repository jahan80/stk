package com.starterkit.auth.auth.application.exception;

public class PasswordResetNotAllowedException extends RuntimeException {

    public PasswordResetNotAllowedException() {
        super("Password reset is not allowed");
    }
}
