package com.starterkit.ticket.ticket.application.exception;

public class GroupAlreadyExistsException extends RuntimeException {
    public GroupAlreadyExistsException(String name) { super("Group exists: " + name); }
}
