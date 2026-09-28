package com.starterkit.ticket.ticket.application.exception;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String from, String to) {
        super("Cannot transition from " + from + " to " + to);
    }
}
