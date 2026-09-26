package com.starterkit.auth.shared.infrastructure.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class NotifEventMessage {
    private final String eventId;
    private final String eventType;
    private final String eventVersion;
    private final String source;
    private final String traceId;
    private final Map<String, Object> data;
    private final Instant occurredAt;

    public static NotifEventMessage sendEmail(
            String to,
            String subject,
            String body,
            String traceId
    ) {
        return NotifEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("SEND_EMAIL")
                .eventVersion("1.0")
                .source("auth-service")
                .traceId(traceId)
                .data(Map.of(
                        "to", to,
                        "subject", subject,
                        "body", body
                ))
                .occurredAt(Instant.now())
                .build();
    }

    public static NotifEventMessage sendSms(
            String to,
            String message,
            String traceId
    ) {
        return NotifEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("SEND_SMS")
                .eventVersion("1.0")
                .source("auth-service")
                .traceId(traceId)
                .data(Map.of(
                        "to", to,
                        "message", message
                ))
                .occurredAt(Instant.now())
                .build();
    }
}
