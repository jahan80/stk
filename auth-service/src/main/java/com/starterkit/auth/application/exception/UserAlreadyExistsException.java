package com.starterkit.auth.application.exception;

import lombok.Getter;

@Getter
public class UserAlreadyExistsException extends RuntimeException {

    private final String field;

    public UserAlreadyExistsException(String field) {
        this.field = field;
    }
}