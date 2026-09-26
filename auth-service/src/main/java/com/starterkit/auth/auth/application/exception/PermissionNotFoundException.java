package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class PermissionNotFoundException extends RuntimeException {

    private final Long permissionId;

    public PermissionNotFoundException(Long permissionId) {
        super("Permission not found: " + permissionId);
        this.permissionId = permissionId;
    }
}
