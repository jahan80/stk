package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class DeletedRoleException extends RuntimeException {

    private final Long roleId;

    public DeletedRoleException(Long roleId) {
        super("Cannot use deleted role: " + roleId);
        this.roleId = roleId;
    }
}
