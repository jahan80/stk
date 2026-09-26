package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class RoleNotFoundException extends RuntimeException {

    private final Long roleId;

    public RoleNotFoundException(Long roleId) {
        super("Role not found: " + roleId);
        this.roleId = roleId;
    }
}
