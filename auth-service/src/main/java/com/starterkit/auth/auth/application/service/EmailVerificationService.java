package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.application.exception.EmailAlreadyVerifiedException;
import com.starterkit.auth.auth.application.exception.InvalidVerificationCodeException;
import com.starterkit.auth.auth.application.event.AuthEventPublisher;
import com.starterkit.auth.auth.application.event.NotifSendEmailEvent;
import com.starterkit.auth.auth.domain.entity.EmailVerificationToken;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.EmailVerificationTokenRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import com.starterkit.auth.configuration.application.ConfigurationService;
import com.starterkit.auth.auth.application.service.VerificationThrottleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigurationService configurationService;
    private final AuthEventPublisher eventPublisher;
    private final VerificationThrottleService throttle;

    @Transactional
    public void sendVerificationCode(User user) {

        // چک اگر email خالیه
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Cannot send verification: user {} has no email", user.getId());
            return;
        }

        // چک اگر قبلاً verify شده
        if (user.isEmailVerified()) {
            log.info("User {} email already verified", user.getId());
            return;
        }

        // A3: enforce per-account cooldown between resends
        throttle.enforceCooldownOrThrow(user, VerificationThrottleService.Channel.EMAIL);

        // پاک کردن tokenهای قدیمی
        tokenRepository.deleteAllByUser(user);
        tokenRepository.flush();   // FIX: ensure DELETE hits DB before new INSERT

        // ساخت کد
        int codeLength = configurationService.getInteger(
                "AUTH.EMAIL.VERIFICATION.CODE.LENGTH");
        String code = generateNumericCode(codeLength);

        // ذخیره hash
        long ttlSeconds = configurationService.getLong(
                "AUTH.EMAIL.VERIFICATION.TOKEN.TTL.SECONDS");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(code));
        token.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));

        tokenRepository.save(token);

        // ارسال event به notif-service
        String subject = "Verify your email";
        String body = buildVerificationEmailBody(user.getUsername(), code, ttlSeconds / 60);

        eventPublisher.publish(new NotifSendEmailEvent(
                user.getEmail(), subject, body, user.getId()));

        log.info("Verification code sent to user {} ({})", user.getId(), user.getEmail());
    }

    @Transactional
    public void verifyCode(String email, String code) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidVerificationCodeException());

        if (user.isEmailVerified()) {
            log.info("User {} email already verified", user.getId());
            return;
        }

        // A3: enforce per-account cooldown between resends
        throttle.enforceCooldownOrThrow(user, VerificationThrottleService.Channel.EMAIL);

        EmailVerificationToken token = tokenRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidVerificationCodeException());

        // چک انقضا
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidVerificationCodeException();
        }

        // چک attempts
        int maxAttempts = configurationService.getInteger(
                "AUTH.EMAIL.VERIFICATION.MAX.ATTEMPTS");

        if (token.getAttempts() >= maxAttempts) {
            throw new InvalidVerificationCodeException();
        }

        // چک کد
        if (!token.getTokenHash().equals(hash(code))) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new InvalidVerificationCodeException();
        }

        // موفق
        token.setVerified(true);
        token.setVerifiedAt(Instant.now());
        tokenRepository.save(token);

        user.setEmailVerified(true);
        userRepository.save(user);

        log.info("Email verified for user {} ({})", user.getId(), user.getEmail());
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

    private String buildVerificationEmailBody(String username, String code, long minutes) {
        return String.format("""
                Hi %s,
                
                Your email verification code is:
                
                    %s
                
                This code will expire in %d minutes.
                
                If you didn't request this, please ignore this email.
                
                - StarterKit Team
                """, username, code, minutes);
    }
}
