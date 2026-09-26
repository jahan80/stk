package com.starterkit.audit.shared.api;

import com.starterkit.audit.shared.api.response.ApiCode;
import com.starterkit.audit.shared.api.response.ApiResponse;
import com.starterkit.audit.shared.api.response.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiResponseFactory responseFactory;

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResource(NoResourceFoundException exception) {
        return responseFactory.error(ApiCode.AUDIT_EVENT_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception);

        return responseFactory.error(ApiCode.INTERNAL_ERROR);
    }
}
