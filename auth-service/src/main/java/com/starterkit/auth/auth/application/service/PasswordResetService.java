package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.application.exception.InvalidPasswordResetCodeException;
import com.starterkit.auth.auth.application.exception.PasswordResetNotAllowedException;
import com.starterkit.auth.auth.application.event.AuthEventPublisher;
import com.starterkit.auth.auth.application.event.NotifSendEmailEvent;
import com.starterkit.auth.auth.domain.entity.PasswordResetToken;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.PasswordResetTokenRepository;
import com.starterkit.auth.auth.domain.repository.RefreshTokenRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import com.starterkit.auth.configuration.application.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ConfigurationService configurationService;
    private final AuthEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final VerificationThrottleService throttle;

    @Transactional
    public void sendResetCode(String email) {

        // چک فعال بودن
        if (!configurationService.getBoolean("AUTH.PASSWORD.RESET.ENABLED")) {
            throw new PasswordResetNotAllowedException();
        }

        // پیدا کردن کاربر (بدون افشا اگر نبود)
        var userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return;  // ← بدون خطا (security)
        }

        User user = userOpt.get();

        // A3: enforce per-account cooldown between password-reset resends
        throttle.enforceCooldownOrThrow(user, VerificationThrottleService.Channel.PASSWORD_RESET);

        // پاک کردن کدهای قدیمی
        tokenRepository.deleteAllByUser(user);
        tokenRepository.flush();   // FIX: ensure DELETE hits DB before new INSERT

        // ساخت کد
        int codeLength = configurationService.getInteger(
                "AUTH.PASSWORD.RESET.CODE.LENGTH");
        String code = generateNumericCode(codeLength);

        long ttlSeconds = configurationService.getLong(
                "AUTH.PASSWORD.RESET.TOKEN.TTL.SECONDS");

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash(code));
        token.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));

        tokenRepository.save(token);

        String subject = "Password Reset";
        String body = buildPasswordResetEmailBody(user.getUsername(), code, ttlSeconds / 60);

        eventPublisher.publish(new NotifSendEmailEvent(
                email, subject, body, user.getId()));

        log.info("Password reset code sent for user {} ({})", user.getId(), email);
    }

    @Transactional
    public void resetPassword(String email, String code, String newPassword) {

        if (!configurationService.getBoolean("AUTH.PASSWORD.RESET.ENABLED")) {
            throw new PasswordResetNotAllowedException();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidPasswordResetCodeException());

        PasswordResetToken token = tokenRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidPasswordResetCodeException());

        if (token.isUsed()) {
            throw new InvalidPasswordResetCodeException();
        }

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidPasswordResetCodeException();
        }

        int maxAttempts = configurationService.getInteger(
                "AUTH.PASSWORD.RESET.MAX.ATTEMPTS");

        if (token.getAttempts() >= maxAttempts) {
            throw new InvalidPasswordResetCodeException();
        }

        if (!token.getTokenHash().equals(hash(code))) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new InvalidPasswordResetCodeException();
        }

        // success - reset
        token.setUsed(true);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // revoke all refresh tokens (optional)
        boolean revokeSessions = false;
        try {
            revokeSessions = configurationService.getBoolean(
                    "AUTH.PASSWORD.RESET.REVOKE.SESSIONS");
        } catch (Exception ignored) {}

        if (revokeSessions) {
            int revoked = refreshTokenRepository.revokeAllByUserId(
                    user.getId(), Instant.now());
            log.info("Revoked {} refresh tokens for user {}", revoked, user.getId());
        }

        log.info("Password reset completed for user {} ({})", user.getId(), email);
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String buildPasswordResetEmailBody(String username, String code, long minutes) {
        return String.format("""
                Hi %s,
                
                Your password reset code is:
                
                    %s
                
                This code will expire in %d minutes.
                
                If you didn't request this, please ignore this email.
                
                - StarterKit Team
                """, username, code, minutes);
    }
}
