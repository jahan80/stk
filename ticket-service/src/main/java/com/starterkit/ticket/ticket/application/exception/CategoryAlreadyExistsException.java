package com.starterkit.ticket.ticket.application.exception;

public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException(String code) {
        super("Category code already exists: " + code);
    }
}
