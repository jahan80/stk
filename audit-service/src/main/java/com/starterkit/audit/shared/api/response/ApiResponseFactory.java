package com.starterkit.audit.shared.api.response;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.time.Instant;


@Component
public class ApiResponseFactory {

    private static final String TRACE_ID_MDC_KEY = "traceId";

    public <T> ApiResponse<T> success(ApiCode apiCode, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(apiCode.getCode())
                .message(apiCode.getDefaultMessage())
                .data(data)
                .timestamp(Instant.now())
                .traceId(getTraceId())
                .build();
    }

    public <T> ApiResponse<T> error(ApiCode apiCode) {
        return error(apiCode, null);
    }

    public <T> ApiResponse<T> error(ApiCode apiCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(apiCode.getCode())
                .message(apiCode.getDefaultMessage())
                .data(data)
                .timestamp(Instant.now())
                .traceId(getTraceId())
                .build();
    }

    private String getTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }
}