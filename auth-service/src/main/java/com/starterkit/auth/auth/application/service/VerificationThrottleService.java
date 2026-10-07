package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.application.exception.VerificationResendTooSoonException;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.EmailVerificationTokenRepository;
import com.starterkit.auth.auth.domain.repository.MobileVerificationTokenRepository;
import com.starterkit.auth.auth.domain.repository.PasswordResetTokenRepository;
import com.starterkit.auth.configuration.application.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Enforces a per-account cooldown between consecutive resend requests.
 *
 * Each verification flow (email, mobile, password reset) deletes old
 * tokens and inserts a fresh one. That insert timestamp
 * (created_at) is the anchor we use to compute "time since last send".
 *
 * Why this service exists:
 *   - Prevents a single account from spamming email/SMS at any rate.
 *   - Reduces provider cost and abuse surface.
 *   - Centralizes the rule so all three flows agree on the semantics.
 *
 * Config keys (all INT, seconds):
 *   AUTH.EMAIL.VERIFICATION.RESEND.COOLDOWN.SECONDS
 *   AUTH.MOBILE.VERIFICATION.RESEND.COOLDOWN.SECONDS
 *   AUTH.PASSWORD.RESET.RESEND.COOLDOWN.SECONDS
 *
 * If a key is missing or disabled, the cooldown is treated as 0
 * (allow immediately). This is intentional so the service is safe
 * to call from tests and from flows that have no cooldown.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationThrottleService {

    public enum Channel {
        EMAIL("AUTH.EMAIL.VERIFICATION.RESEND.COOLDOWN.SECONDS"),
        MOBILE("AUTH.MOBILE.VERIFICATION.RESEND.COOLDOWN.SECONDS"),
        PASSWORD_RESET("AUTH.PASSWORD.RESET.RESEND.COOLDOWN.SECONDS");

        public final String configKey;

        Channel(String configKey) {
            this.configKey = configKey;
        }
    }

    private final EmailVerificationTokenRepository emailTokens;
    private final MobileVerificationTokenRepository mobileTokens;
    private final PasswordResetTokenRepository passwordTokens;
    private final ConfigurationService configurationService;

    /**
     * Throws {@link VerificationResendTooSoonException} if the user is
     * still inside the cooldown window for the given channel.
     */
    @Transactional(readOnly = true)
    public void enforceCooldownOrThrow(User user, Channel channel) {

        long cooldownSeconds = readCooldownSeconds(channel);
        if (cooldownSeconds <= 0) {
            return;
        }

        Optional<Instant> lastSentAt = lastSentAt(user, channel);
        if (lastSentAt.isEmpty()) {
            return; // first send — allowed
        }

        long elapsedSeconds = Duration.between(lastSentAt.get(), Instant.now()).getSeconds();
        long remaining = cooldownSeconds - elapsedSeconds;

        if (remaining > 0) {
            log.info("Cooldown active for channel={}, userId={}, remaining={}s",
                    channel, user.getId(), remaining);
            throw new VerificationResendTooSoonException(channel.name(), remaining);
        }
    }

    /**
     * Read configured cooldown; 0 if missing or disabled.
     */
    private long readCooldownSeconds(Channel channel) {
        try {
            return configurationService.getLong(channel.configKey);
        } catch (Exception ex) {
            log.debug("No cooldown configured for {} ({}), defaulting to 0",
                    channel, channel.configKey);
            return 0L;
        }
    }

    /**
     * Return the most recent token creation timestamp for the channel.
     */
    private Optional<Instant> lastSentAt(User user, Channel channel) {
        return switch (channel) {
            case EMAIL -> emailTokens
                    .findTopByUserOrderByCreatedAtDesc(user)
                    .map(t -> t.getCreatedAt());
            case MOBILE -> mobileTokens
                    .findTopByUserOrderByCreatedAtDesc(user)
                    .map(t -> t.getCreatedAt());
            case PASSWORD_RESET -> passwordTokens
                    .findTopByUserOrderByCreatedAtDesc(user)
                    .map(t -> t.getCreatedAt());
        };
    }
}
