package com.starterkit.audit.shared.api;

import com.starterkit.audit.shared.api.response.ApiCode;
import com.starterkit.commons.web.ApiResponse;
import com.starterkit.commons.web.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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

    /**
     * FIX P1-4: method-security failures must return 403, not 500.
     *
     * Spring Security 6 throws AuthorizationDeniedException (subclass of
     * AccessDeniedException) when @PreAuthorize fails. Without an explicit
     * handler, @RestControllerAdvice's catch-all would turn it into 500.
     * Downstream from the gateway this is still hidden from clients, but
     * it breaks local testing and corrupts the semantics.
     */
    @ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(Exception exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return responseFactory.error(ApiCode.FORBIDDEN);
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
