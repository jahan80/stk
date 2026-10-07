package com.starterkit.ticket.ticket.application.exception;

public class CategoryInUseException extends RuntimeException {
    private final long ticketCount;

    public CategoryInUseException(Long categoryId, long ticketCount) {
        super("Category " + categoryId + " is used by " + ticketCount + " ticket(s)");
        this.ticketCount = ticketCount;
    }

    public long getTicketCount() { return ticketCount; }
}
