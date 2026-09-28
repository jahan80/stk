package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class TicketResponse {
    private final Long id;
    private final String ticketNumber;
    private final String title;
    private final String description;
    private final String status;
    private final String priority;
    private final Long categoryId;
    private final String categoryName;
    private final String categoryCode;
    private final Long groupId;
    private final String groupName;
    private final Long createdBy;
    private final Long assignedTo;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant resolvedAt;
    private final Instant closedAt;
    private final Instant slaDeadline;
    private final String viewerRole;   // ADMIN, AGENT, or USER
    private final long commentCount;
    private final List<CommentResponse> comments;
}
