package com.starterkit.ticket.ticket.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {
    private final Long id;
    private final String type;
    private final String title;
    private final String message;
    private final Long ticketId;
    private final Long actorId;
    private final boolean read;
    private final Instant readAt;
    private final String link;
    private final Instant createdAt;
}
