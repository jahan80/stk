package com.starterkit.gateway.event;

import com.starterkit.gateway.filter.RequestContext;

import java.util.HashMap;
import java.util.Map;

public record RequestReceivedEvent(RequestContext ctx) implements GatewayEvent {

    @Override
    public String eventType() {
        return "REQUEST_RECEIVED";
    }

    @Override
    public String routingKey() {
        return "gateway.request.received";
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("requestId", ctx.requestId());
        data.put("traceId", ctx.traceId());
        data.put("method", ctx.method());
        data.put("path", ctx.path());
        data.put("query", ctx.query());
        data.put("routeId", ctx.routeId());
        data.put("targetService", ctx.targetService());
        data.put("targetUri", ctx.targetUri());
        data.put("clientIp", ctx.clientIp());
        data.put("userAgent", ctx.userAgent());
        data.put("userId", ctx.userId());
        data.put("username", ctx.username());
        data.put("roles", ctx.roles());
        return data;
    }
}
