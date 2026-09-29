package com.starterkit.ticket.ticket.application.exception;

public class TicketAccessDeniedException extends RuntimeException {
    public TicketAccessDeniedException() { super("Access denied"); }
}
