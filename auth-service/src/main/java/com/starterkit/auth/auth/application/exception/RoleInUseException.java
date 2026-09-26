package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

@Getter
public class RoleInUseException extends RuntimeException {

    private final Long roleId;
    private final long userCount;

    public RoleInUseException(Long roleId, long userCount) {
        super("Role " + roleId + " is assigned to " + userCount + " users");
        this.roleId = roleId;
        this.userCount = userCount;
    }
}
