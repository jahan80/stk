package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;

@Getter
@Builder
public class RoleResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final boolean systemRole;
    private final Instant createdAt;
    private final Set<PermissionResponse> permissions;
}
