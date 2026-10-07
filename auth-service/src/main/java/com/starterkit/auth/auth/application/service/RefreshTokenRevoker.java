package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * FIX P1-3 — Executes refresh-token revocation in a NEW transaction,
 * independent of the caller's transaction.
 *
 * Why a separate bean?
 *   @Transactional(REQUIRES_NEW) on a *self-invoked* method is NOT
 *   honored by Spring's proxy-based AOP: the call never leaves the
 *   bean, so the proxy never wraps it. The old TokenService.refresh()
 *   called revokeAllTokensForUser() on `this`; the REQUIRES_NEW
 *   annotation was ignored, the revocation ran in the same tx as the
 *   refresh, and the subsequent throw rolled it back → reuse detection
 *   was effectively disabled.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int revokeAllForUser(Long userId) {
        int revoked = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.warn("Revoked {} refresh tokens for user {}", revoked, userId);
        return revoked;
    }
}
