package com.starterkit.auth.auth.application.event;

public interface AuthEventPublisher {
    void publish(AuthEvent event);
}
