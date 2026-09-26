package com.starterkit.audit.audit.api.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * Message received from RabbitMQ.
 * Field names match auth-service publisher.
 */
@Getter
@Setter
@NoArgsConstructor
public class AuthEventMessage {

    private String eventId;
    private String eventType;
    private String eventVersion;
    private String source;
    private String traceId;
    private Map<String, Object> data;
    private Instant occurredAt;
}
