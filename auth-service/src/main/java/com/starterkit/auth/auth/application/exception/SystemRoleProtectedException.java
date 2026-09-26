package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class SystemRoleProtectedException extends RuntimeException {

    private final String roleName;
    private final String action;

    public SystemRoleProtectedException(String roleName, String action) {
        super("System role '" + roleName + "' cannot be " + action);
        this.roleName = roleName;
        this.action = action;
    }
}
