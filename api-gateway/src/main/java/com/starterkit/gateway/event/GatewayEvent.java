package com.starterkit.gateway.event;

import java.util.Map;

public interface GatewayEvent {
    String eventType();
    String routingKey();
    Map<String, Object> data();
}
