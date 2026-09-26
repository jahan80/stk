package com.starterkit.auth.auth.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PermissionResponse {
    private final Long id;
    private final String code;
    private final String description;
}
