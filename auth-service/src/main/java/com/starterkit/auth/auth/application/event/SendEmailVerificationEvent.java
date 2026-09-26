package com.starterkit.auth.auth.application.event;

public record SendEmailVerificationEvent(
        String email,
        String username,
        String verificationCode
) {}
