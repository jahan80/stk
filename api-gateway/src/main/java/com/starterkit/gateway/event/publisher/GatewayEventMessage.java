package com.starterkit.gateway.event.publisher;

import com.starterkit.gateway.event.GatewayEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class GatewayEventMessage {
    private final String eventId;
    private final String eventType;
    private final String eventVersion;
    private final String source;
    private final String traceId;
    private final Map<String, Object> data;
    private final Instant occurredAt;

    public static GatewayEventMessage from(GatewayEvent event) {
        String traceId = event.data().get("traceId") != null
                ? event.data().get("traceId").toString()
                : null;

        return GatewayEventMessage.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(event.eventType())
                .eventVersion("1.0")
                .source("api-gateway")
                .traceId(traceId)
                .data(event.data())
                .occurredAt(Instant.now())
                .build();
    }
}
