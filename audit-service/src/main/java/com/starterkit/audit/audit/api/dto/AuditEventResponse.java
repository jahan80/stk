package com.starterkit.audit.audit.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class AuditEventResponse {
    private Long id;
    private UUID eventId;
    private String eventType;
    private String eventVersion;
    private String source;
    private String traceId;
    private Map<String, Object> payload;
    private Instant occurredAt;
    private Instant receivedAt;
}
