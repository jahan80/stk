package com.starterkit.auth.auth.application.event;

import java.util.HashMap;
import java.util.Map;

public record UserLoggedOutEvent(
        Long userId,
        String username
) implements AuthEvent {

    @Override
    public String eventType() {
        return "USER_LOGGED_OUT";
    }

    @Override
    public String routingKey() {
        return "auth.user.logged-out";
    }

    @Override
    public Map<String, Object> data() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("username", username);
        return data;
    }
}
