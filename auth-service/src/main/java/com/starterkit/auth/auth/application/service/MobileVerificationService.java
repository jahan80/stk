package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.application.exception.InvalidMobileVerificationCodeException;
import com.starterkit.auth.auth.application.exception.MobileAlreadyVerifiedException;
import com.starterkit.auth.auth.application.event.AuthEventPublisher;
import com.starterkit.auth.auth.application.event.NotifSendSmsEvent;
import com.starterkit.auth.auth.domain.entity.MobileVerificationToken;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.MobileVerificationTokenRepository;
import com.starterkit.auth.auth.domain.repository.UserRepository;
import com.starterkit.auth.configuration.application.ConfigurationService;
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
public class MobileVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MobileVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ConfigurationService configurationService;
    private final AuthEventPublisher eventPublisher;

    @Transactional
    public void sendVerificationCode(User user) {

        if (user.getMobileNumber() == null || user.getMobileNumber().isBlank()) {
            log.warn("Cannot send mobile verification: user {} has no mobile", user.getId());
            return;
        }

        if (user.isMobileVerified()) {
            log.info("User {} mobile already verified", user.getId());
            return;
        }

        tokenRepository.deleteAllByUser(user);
        tokenRepository.flush();   // FIX: ensure DELETE hits DB before new INSERT

        int codeLength = configurationService.getInteger(
                "AUTH.MOBILE.VERIFICATION.CODE.LENGTH");
        String code = generateNumericCode(codeLength);

        long ttlSeconds = configurationService.getLong(
                "AUTH.MOBILE.VERIFICATION.TOKEN.TTL.SECONDS");

        MobileVerificationToken token = new MobileVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(code));
        token.setExpiresAt(Instant.now().plusSeconds(ttlSeconds));

        tokenRepository.save(token);

        String message = String.format(
                "StarterKit verification code: %s. Valid for %d minutes.",
                code, ttlSeconds / 60
        );

        eventPublisher.publish(new NotifSendSmsEvent(
                user.getMobileNumber(), message, user.getId()));

        log.info("Mobile verification code sent to user {} ({})",
                user.getId(), user.getMobileNumber());
    }

    @Transactional
    public void verifyCode(String mobile, String code) {

        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new InvalidMobileVerificationCodeException());

        if (user.isMobileVerified()) {
            throw new MobileAlreadyVerifiedException(mobile);
        }

        MobileVerificationToken token = tokenRepository
                .findTopByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidMobileVerificationCodeException());

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidMobileVerificationCodeException();
        }

        int maxAttempts = configurationService.getInteger(
                "AUTH.MOBILE.VERIFICATION.MAX.ATTEMPTS");

        if (token.getAttempts() >= maxAttempts) {
            throw new InvalidMobileVerificationCodeException();
        }

        if (!token.getTokenHash().equals(hash(code))) {
            token.setAttempts(token.getAttempts() + 1);
            tokenRepository.save(token);
            throw new InvalidMobileVerificationCodeException();
        }

        token.setVerified(true);
        token.setVerifiedAt(Instant.now());
        tokenRepository.save(token);

        user.setMobileVerified(true);
        userRepository.save(user);

        log.info("Mobile verified for user {} ({})", user.getId(), user.getMobileNumber());
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
}
