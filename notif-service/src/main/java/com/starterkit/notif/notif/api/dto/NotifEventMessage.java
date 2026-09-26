package com.starterkit.notif.notif.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Event received from RabbitMQ.
 * Matches auth-service / api-gateway event format.
 */
@Getter
@Setter
@NoArgsConstructor
public class NotifEventMessage {

    private String eventId;
    private String eventType;
    private String eventVersion;
    private String source;
    private String traceId;
    private Map<String, Object> data;
    private Instant occurredAt;
}
