package com.starterkit.auth.auth.application.exception;

public class InvalidMobileVerificationCodeException extends RuntimeException {

    public InvalidMobileVerificationCodeException() {
        super("Invalid or expired mobile verification code");
    }
}
