package com.starterkit.auth.auth.application.event;

import java.util.Map;

public interface AuthEvent {
    String eventType();
    String routingKey();
    Map<String, Object> data();
}
