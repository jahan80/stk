package com.starterkit.audit.shared.api.response;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final Instant timestamp;
    private final String traceId;
}