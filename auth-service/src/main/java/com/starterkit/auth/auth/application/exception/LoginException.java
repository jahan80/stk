package com.starterkit.auth.auth.application.exception;

import com.starterkit.auth.shared.api.response.ApiCode;
import lombok.Getter;

@Getter
public class LoginException extends RuntimeException {

    private final ApiCode apiCode;

    public LoginException(ApiCode apiCode) {
        super(apiCode.getDefaultMessage());
        this.apiCode = apiCode;
    }
}