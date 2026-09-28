package com.starterkit.ticket.shared.api.response;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApiResponseFactory {

    private static final String TRACE = "traceId";

    public <T> ApiResponse<T> success(ApiCode code, T data) {
        return build(true, code, data);
    }

    public <T> ApiResponse<T> error(ApiCode code) {
        return build(false, code, null);
    }

    public <T> ApiResponse<T> error(ApiCode code, T data) {
        return build(false, code, data);
    }

    private <T> ApiResponse<T> build(boolean success, ApiCode code, T data) {
        return ApiResponse.<T>builder()
                .success(success)
                .code(code.getCode())
                .message(code.getMessage())
                .data(data)
                .timestamp(Instant.now())
                .traceId(MDC.get(TRACE))
                .build();
    }
}
