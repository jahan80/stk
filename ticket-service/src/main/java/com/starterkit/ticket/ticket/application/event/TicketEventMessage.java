package com.starterkit.ticket.ticket.application.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class TicketEventMessage {
    private final String eventId;
    private final String eventType;
    private final String eventVersion;
    private final String source;
    private final String traceId;
    private final Map<String, Object> data;
    private final Instant occurredAt;

    public static TicketEventMessage from(TicketEvent event, String traceId) {
        return TicketEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(event.eventType())
                .eventVersion("1.0")
                .source("ticket-service")
                .traceId(traceId)
                .data(event.data())
                .occurredAt(Instant.now())
                .build();
    }
}
