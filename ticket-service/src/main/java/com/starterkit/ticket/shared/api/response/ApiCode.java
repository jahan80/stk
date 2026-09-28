package com.starterkit.ticket.shared.api.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ApiCode {
    SUCCESS("TICKET-000", "Operation completed successfully"),

    // Ticket
    TICKET_CREATED("TICKET-001", "Ticket created successfully"),
    TICKET_NOT_FOUND("TICKET-002", "Ticket not found"),
    TICKET_ACCESS_DENIED("TICKET-003", "You don't have access to this ticket"),
    TICKET_INVALID_STATUS_TRANSITION("TICKET-004", "Invalid status transition"),
    TICKET_ALREADY_CLOSED("TICKET-005", "Ticket is already closed"),

    // Category
    CATEGORY_NOT_FOUND("TICKET-CAT-001", "Category not found"),
    CATEGORY_ALREADY_EXISTS("TICKET-CAT-002", "Category already exists"),

    // Group
    GROUP_NOT_FOUND("TICKET-GRP-001", "Group not found"),
    GROUP_ALREADY_EXISTS("TICKET-GRP-002", "Group already exists"),
    GROUP_MEMBER_ALREADY_EXISTS("TICKET-GRP-003", "User is already a member"),
    GROUP_MEMBER_NOT_FOUND("TICKET-GRP-004", "Member not found"),

    // Comment
    COMMENT_ADDED("TICKET-CMT-001", "Comment added successfully"),

    // Validation/Server
    VALIDATION_ERROR("TICKET-VAL-001", "Request validation failed"),
    FORBIDDEN("TICKET-FORBIDDEN", "Access denied"),
    INTERNAL_ERROR("TICKET-500", "An unexpected error occurred");

    private final String code;
    private final String message;
}
