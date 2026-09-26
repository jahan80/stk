package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class RoleAlreadyExistsException extends RuntimeException {

    private final String roleName;

    public RoleAlreadyExistsException(String roleName) {
        super("Role already exists: " + roleName);
        this.roleName = roleName;
    }
}
