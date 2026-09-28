package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class TicketSummaryResponse {
    private final Long id;
    private final String ticketNumber;
    private final String title;
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
}
