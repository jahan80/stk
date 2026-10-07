package com.starterkit.auth.auth.application.exception;

import lombok.Getter;

/**
 * Thrown when a resend request arrives before the configured cooldown
 * has elapsed. Carries the remaining seconds so the HTTP layer can
 * return a Retry-After header.
 */
@Getter
public class VerificationResendTooSoonException extends RuntimeException {

    private final long remainingSeconds;
    private final String channel;

    public VerificationResendTooSoonException(String channel, long remainingSeconds) {
        super("Resend too soon for " + channel + ": " + remainingSeconds + "s remaining");
        this.channel = channel;
        this.remainingSeconds = remainingSeconds;
    }
}
