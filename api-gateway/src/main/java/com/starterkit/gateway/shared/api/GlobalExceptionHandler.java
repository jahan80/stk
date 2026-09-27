package com.starterkit.gateway.shared.api;

import com.starterkit.gateway.shared.api.response.ApiCode;
import com.starterkit.gateway.shared.api.response.ApiResponse;
import com.starterkit.gateway.shared.api.response.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiResponseFactory responseFactory;

    // =====================================================
    // Business
    // =====================================================

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("Not found: {}", exception.getMessage());
        return responseFactory.error(ApiCode.RATE_LIMIT_NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleIllegalState(IllegalStateException exception) {
        log.warn("Conflict: {}", exception.getMessage());
        return responseFactory.error(ApiCode.RATE_LIMIT_ALREADY_EXISTS);
    }

    // =====================================================
    // Validation
    // =====================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage(),
                        (existing, replacement) -> existing
                ));

        return responseFactory.error(
                ApiCode.VALIDATION_ERROR,
                Map.of("fields", fields)
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleServerWebInput(ServerWebInputException exception) {
        log.warn("Invalid input: {}", exception.getMessage());
        return responseFactory.error(ApiCode.VALIDATION_ERROR);
    }

    // =====================================================
    // Catch-all (WebFlux)
    // =====================================================

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(
            Exception exception,
            ServerWebExchange exchange
    ) {
        log.error("Unhandled exception at {} {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                exception);

        return responseFactory.error(ApiCode.INTERNAL_ERROR);
    }
}
