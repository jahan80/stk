package com.starterkit.auth.shared.infrastructure.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Message published to RabbitMQ.
 * Must match audit-service's AuthEventMessage DTO.
 */
@Getter
@Builder
public class AuthEventMessage {
    private final String eventId;
    private final String eventType;
    private final String eventVersion;
    private final String source;
    private final String traceId;
    private final Map<String, Object> data;
    private final Instant occurredAt;

    public static AuthEventMessage from(
            com.starterkit.auth.auth.application.event.AuthEvent event,
            String traceId
    ) {
        return AuthEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(event.eventType())
                .eventVersion("1.0")
                .source("auth-service")
                .traceId(traceId)
                .data(event.data())
                .occurredAt(Instant.now())
                .build();
    }
}
