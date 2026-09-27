package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class MobileAlreadyVerifiedException extends RuntimeException {

    private final String mobile;

    public MobileAlreadyVerifiedException(String mobile) {
        super("Mobile already verified: " + mobile);
        this.mobile = mobile;
    }
}
