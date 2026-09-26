package com.starterkit.auth.auth.application.event;

import java.util.HashMap;
import java.util.Map;

public record UserRegisteredEvent(
        Long userId,
        String username,
        String email
) implements AuthEvent {

    @Override
    public String eventType() {
        return "USER_REGISTERED";
    }

    @Override
    public String routingKey() {
        return "auth.user.registered";
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        data.put("email", email);
        return data;
    }
}
