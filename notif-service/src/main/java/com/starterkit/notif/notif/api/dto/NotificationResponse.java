package com.starterkit.notif.notif.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {

    private final UUID notificationId;
    private final String channel;
    private final String recipient;
    private final String subject;
    private final String status;
    private final String provider;
    private final String providerMessageId;
    private final String errorMessage;
    private final Map<String, Object> metadata;
    private final Instant createdAt;
    private final Instant sentAt;
}
