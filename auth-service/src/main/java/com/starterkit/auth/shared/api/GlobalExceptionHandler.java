package com.starterkit.auth.shared.api;

import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.api.response.ApiResponse;
import com.starterkit.auth.shared.api.response.ApiResponseFactory;
import com.starterkit.auth.shared.api.response.ValidationErrorResponse;
import com.starterkit.auth.auth.application.exception.DeletedRoleException;
import com.starterkit.auth.auth.application.exception.LoginException;
import com.starterkit.auth.auth.application.exception.PermissionNotFoundException;
import com.starterkit.auth.auth.application.exception.RoleAlreadyExistsException;
import com.starterkit.auth.auth.application.exception.RoleInUseException;
import com.starterkit.auth.auth.application.exception.RoleNotFoundException;
import com.starterkit.auth.auth.application.exception.SystemRoleProtectedException;
import com.starterkit.auth.auth.application.exception.UserNotFoundException;
import com.starterkit.auth.auth.application.exception.UserAlreadyExistsException;
import com.starterkit.auth.configuration.exception.ConfigurationAlreadyExistsException;
import com.starterkit.auth.configuration.exception.ConfigurationNotFoundException;
import com.starterkit.auth.configuration.exception.ConfigurationValidationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiResponseFactory responseFactory;

    // =========================================================
    // 1. Domain exceptions
    // =========================================================

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleUserAlreadyExists(
            UserAlreadyExistsException exception
    ) {
        ApiCode apiCode = switch (exception.getField()) {
            case "username" -> ApiCode.USERNAME_ALREADY_EXISTS;
            case "email" -> ApiCode.EMAIL_ALREADY_EXISTS;
            case "mobileNumber" -> ApiCode.MOBILE_ALREADY_EXISTS;
            default -> ApiCode.USER_FIELD_UNKNOWN;
        };

        log.warn("User already exists: field={}", exception.getField());

        return responseFactory.error(apiCode);
    }

    @ExceptionHandler(LoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoginException(
            LoginException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = switch (exception.getApiCode()) {
            case LOGIN_DISABLED,
                 LOGIN_IDENTIFIER_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case USER_DISABLED,
                 INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };

        // لاگ امن: identifier را نمی‌نویسیم
        log.warn("Login failed: code={}, path={}",
                exception.getApiCode().getCode(),
                request.getRequestURI());

        return ResponseEntity
                .status(status)
                .body(responseFactory.error(exception.getApiCode()));
    }

    @ExceptionHandler(ConfigurationAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConfigurationAlreadyExists(
            ConfigurationAlreadyExistsException exception
    ) {
        log.warn("Configuration already exists: key={}",
                exception.getConfigKey());

        return responseFactory.error(ApiCode.CONFIGURATION_ALREADY_EXISTS);
    }

    @ExceptionHandler(ConfigurationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleConfigurationNotFound(
            ConfigurationNotFoundException exception
    ) {
        log.warn("Configuration not found: key={}",
                exception.getConfigKey());

        return responseFactory.error(ApiCode.CONFIGURATION_NOT_FOUND);
    }

    // =========================================================
    // 2. Validation exceptions
    // =========================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ValidationErrorResponse> handleValidationException(
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

        ValidationErrorResponse data = ValidationErrorResponse.builder()
                .fields(fields)
                .build();

        return responseFactory.error(ApiCode.VALIDATION_ERROR, data);
    }

    @ExceptionHandler(ConfigurationValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<ValidationErrorResponse> handleConfigurationValidation(
            ConfigurationValidationException exception
    ) {
        Map<String, String> fields = new HashMap<>();
        fields.put(exception.getField(), exception.getMessage());

        ValidationErrorResponse data = ValidationErrorResponse.builder()
                .fields(fields)
                .build();

        return responseFactory.error(ApiCode.VALIDATION_ERROR, data);
    }

    // =========================================================
    // 3. Request / Protocol exceptions
    // =========================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidRequest(
            HttpMessageNotReadableException exception
    ) {
        log.warn("Malformed request body: {}", exception.getMessage());

        return responseFactory.error(ApiCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception
    ) {
        log.warn("Method not supported: {}", exception.getMethod());

        return responseFactory.error(ApiCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ApiResponse<Void> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception
    ) {
        log.warn("Media type not supported: {}",
                exception.getContentType());

        return responseFactory.error(ApiCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNoResourceFound(
            NoResourceFoundException exception
    ) {
        // 404 برای مسیرهای ناشناخته (مثلاً /favicon.ico) — بدون لاگ سنگین
        log.debug("Resource not found: {}", exception.getResourcePath());

        return responseFactory.error(ApiCode.RESOURCE_NOT_FOUND);
    }

    // =========================================================
    // 4. Database exceptions
    // =========================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        ApiCode apiCode = resolveDataIntegrityError(exception);

        HttpStatus status = apiCode == ApiCode.INTERNAL_ERROR
                ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.CONFLICT;

        if (apiCode == ApiCode.INTERNAL_ERROR) {
            log.error("Unhandled data integrity violation", exception);
        } else {
            log.warn("Data integrity violation: code={}",
                    apiCode.getCode());
        }

        return ResponseEntity
                .status(status)
                .body(responseFactory.error(apiCode));
    }

    /**
     * Detect constraint violation by PostgreSQL constraint name.
     * Uses PSQLException#getServerErrorMessage() for reliability.
     */
    private ApiCode resolveDataIntegrityError(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception.getMostSpecificCause();

        if (cause instanceof PSQLException psqlException
                && psqlException.getServerErrorMessage() != null) {

            String constraint = psqlException
                    .getServerErrorMessage()
                    .getConstraint();

            if (constraint != null) {
                return switch (constraint) {
                    case "users_username_key" -> ApiCode.USERNAME_ALREADY_EXISTS;
                    case "users_email_key" -> ApiCode.EMAIL_ALREADY_EXISTS;
                    case "users_mobile_number_key" -> ApiCode.MOBILE_ALREADY_EXISTS;
                    case "configurations_config_key_key" -> ApiCode.CONFIGURATION_ALREADY_EXISTS;
                    default -> ApiCode.INTERNAL_ERROR;
                };
            }
        }

        // fallback: message-based detection (قدیمی)
        String message = exception.getMostSpecificCause().getMessage();

        if (message != null) {
            if (message.contains("users_username_key"))
                return ApiCode.USERNAME_ALREADY_EXISTS;
            if (message.contains("users_email_key"))
                return ApiCode.EMAIL_ALREADY_EXISTS;
            if (message.contains("users_mobile_number_key"))
                return ApiCode.MOBILE_ALREADY_EXISTS;
        }

        return ApiCode.INTERNAL_ERROR;
    }

    // =========================================================
    // 5. Catch-all (آخرین خط دفاعی)
    // =========================================================

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

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDenied(AccessDeniedException exception) {
        log.warn("Access denied: {}", exception.getMessage());
        return responseFactory.error(ApiCode.FORBIDDEN);
    }



    // =========================================================
    // 6. Role / Permission / User exceptions
    // =========================================================

    @ExceptionHandler(RoleNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleRoleNotFound(RoleNotFoundException exception) {
        log.warn("Role not found: {}", exception.getRoleId());
        return responseFactory.error(ApiCode.ROLE_NOT_FOUND);
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleRoleAlreadyExists(RoleAlreadyExistsException exception) {
        log.warn("Role already exists: {}", exception.getRoleName());
        return responseFactory.error(ApiCode.ROLE_ALREADY_EXISTS);
    }

    @ExceptionHandler(RoleInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleRoleInUse(RoleInUseException exception) {
        log.warn("Role in use: id={}, users={}", exception.getRoleId(), exception.getUserCount());
        return responseFactory.error(ApiCode.ROLE_IN_USE);
    }

    @ExceptionHandler(SystemRoleProtectedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleSystemRoleProtected(SystemRoleProtectedException exception) {
        log.warn("System role protected: name={}, action={}", exception.getRoleName(), exception.getAction());
        return responseFactory.error(ApiCode.ROLE_SYSTEM_PROTECTED);
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleUserNotFound(UserNotFoundException exception) {
        log.warn("User not found: {}", exception.getUserId());
        return responseFactory.error(ApiCode.USER_NOT_FOUND);
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handlePermissionNotFound(PermissionNotFoundException exception) {
        log.warn("Permission not found: {}", exception.getPermissionId());
        return responseFactory.error(ApiCode.PERMISSION_NOT_FOUND);
    }


    @ExceptionHandler(DeletedRoleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDeletedRole(DeletedRoleException exception) {
        log.warn("Deleted role used: {}", exception.getRoleId());
        return responseFactory.error(ApiCode.ROLE_DELETED);
    }

}
