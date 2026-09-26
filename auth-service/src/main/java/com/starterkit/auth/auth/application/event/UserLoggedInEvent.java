package com.starterkit.auth.auth.application.event;

import java.util.HashMap;
import java.util.Map;

public record UserLoggedInEvent(
        Long userId,
        String username,
        String identifier
) implements AuthEvent {

    @Override
    public String eventType() {
        return "USER_LOGGED_IN";
    }

    @Override
    public String routingKey() {
        return "auth.user.logged-in";
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        data.put("identifier", identifier);
        return data;
    }
}
