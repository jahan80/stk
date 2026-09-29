package com.starterkit.ticket.shared.api;

import com.starterkit.ticket.shared.api.response.ApiCode;
import com.starterkit.ticket.shared.api.response.ApiResponse;
import com.starterkit.ticket.shared.api.response.ApiResponseFactory;
import com.starterkit.ticket.ticket.application.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiResponseFactory factory;

    @ExceptionHandler(TicketNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleTicketNotFound(TicketNotFoundException e) {
        return factory.error(ApiCode.TICKET_NOT_FOUND);
    }

    @ExceptionHandler(TicketAccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccess(TicketAccessDeniedException e) {
        return factory.error(ApiCode.TICKET_ACCESS_DENIED);
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleCat(CategoryNotFoundException e) {
        return factory.error(ApiCode.CATEGORY_NOT_FOUND);
    }

    @ExceptionHandler(GroupNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleGrp(GroupNotFoundException e) {
        return factory.error(ApiCode.GROUP_NOT_FOUND);
    }

    @ExceptionHandler(GroupAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleGrpExists(GroupAlreadyExistsException e) {
        return factory.error(ApiCode.GROUP_ALREADY_EXISTS);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleStatus(InvalidStatusTransitionException e) {
        return factory.error(ApiCode.TICKET_INVALID_STATUS_TRANSITION);
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleIllegalState(IllegalStateException e) {
        return factory.error(ApiCode.TICKET_ALREADY_CLOSED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "Invalid" : fe.getDefaultMessage(),
                        (a, b) -> a));
        return factory.error(ApiCode.VALIDATION_ERROR, Map.of("fields", fields));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGeneric(Exception e) {
        log.error("Unhandled", e);
        return factory.error(ApiCode.INTERNAL_ERROR);
    }
}
