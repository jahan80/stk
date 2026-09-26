package com.starterkit.gateway.event;

import java.util.HashMap;
import java.util.Map;

public record ResponseSentEvent(
        String requestId,
        String traceId,
        String method,
        String path,
        String targetService,
        int status,
        long durationMs,
        long responseSize,
        Long userId,
        String errorCode,
        String errorMessage
) implements GatewayEvent {

    @Override
    public String eventType() {
        return "RESPONSE_SENT";
    }

    @Override
    public String routingKey() {
        return "gateway.response.sent";
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", requestId);
        data.put("traceId", traceId);
        data.put("method", method);
        data.put("path", path);
        data.put("targetService", targetService);
        data.put("status", status);
        data.put("durationMs", durationMs);
        data.put("responseSize", responseSize);
        data.put("userId", userId);
        data.put("success", status >= 200 && status < 400);
        if (errorCode != null) data.put("errorCode", errorCode);
        if (errorMessage != null) data.put("errorMessage", errorMessage);
        return data;
    }
}
