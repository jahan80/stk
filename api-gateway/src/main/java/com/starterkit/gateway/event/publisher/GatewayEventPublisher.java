package com.starterkit.gateway.event.publisher;

import com.starterkit.gateway.event.GatewayEvent;

public interface GatewayEventPublisher {
    void publish(GatewayEvent event);
}
