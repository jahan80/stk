package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class MobileNotVerifiedException extends RuntimeException {

    private final String mobile;

    public MobileNotVerifiedException(String mobile) {
        super("Mobile not verified: " + mobile);
        this.mobile = mobile;
    }
}
