package com.starterkit.ticket.ticket.application.exception;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(Long id) { super("Group not found: " + id); }
}
