package com.starterkit.auth.auth.application.service;

import com.starterkit.auth.auth.application.exception.LoginException;
import com.starterkit.auth.auth.domain.entity.RefreshToken;
import com.starterkit.auth.auth.domain.entity.User;
import com.starterkit.auth.auth.domain.repository.RefreshTokenRepository;
import com.starterkit.auth.shared.api.response.ApiCode;
import com.starterkit.auth.shared.infrastructure.jwt.JwtProperties;
import com.starterkit.auth.shared.infrastructure.jwt.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenPair generateTokens(User user) {

        String accessToken = jwtService.generateAccessToken(user);

        String refreshTokenValue = generateRandomToken();
        String tokenHash = hashToken(refreshTokenValue);
        String jti = jwtService.generateRefreshTokenJti();
        Instant expiresAt = Instant.now()
                .plusSeconds(jwtProperties.getRefreshTokenTtlSeconds());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setJti(jti);
        refreshToken.setExpiresAt(expiresAt);

        refreshTokenRepository.save(refreshToken);

        return new TokenPair(
                accessToken,
                refreshTokenValue,
                jwtProperties.getAccessTokenTtlSeconds()
        );
    }

    @Transactional
    public TokenPair refresh(String refreshTokenValue) {

        String tokenHash = hashToken(refreshTokenValue);

        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));

        if (existing.isRevoked()) {
            throw new LoginException(ApiCode.INVALID_CREDENTIALS);
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new LoginException(ApiCode.INVALID_CREDENTIALS);
        }

        // Revoke old token
        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());

        // Generate new pair
        String newRefreshValue = generateRandomToken();
        String newHash = hashToken(newRefreshValue);
        String newJti = jwtService.generateRefreshTokenJti();

        existing.setReplacedBy(newJti);
        refreshTokenRepository.save(existing);

        String newAccessToken = jwtService.generateAccessToken(existing.getUser());

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(existing.getUser());
        newToken.setTokenHash(newHash);
        newToken.setJti(newJti);
        newToken.setExpiresAt(Instant.now()
                .plusSeconds(jwtProperties.getRefreshTokenTtlSeconds()));
        refreshTokenRepository.save(newToken);

        return new TokenPair(
                newAccessToken,
                newRefreshValue,
                jwtProperties.getAccessTokenTtlSeconds()
        );
    }

    public Claims getClaims(String accessToken) {
        return jwtService.parse(accessToken);
    }

    private String generateRandomToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long expiresInSeconds
    ) {}

    @Transactional
    public void logout(String refreshTokenValue) {

        String tokenHash = hashToken(refreshTokenValue);

        RefreshToken existing = refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() -> new LoginException(ApiCode.INVALID_CREDENTIALS));

        if (!existing.isRevoked()) {
            existing.setRevoked(true);
            existing.setRevokedAt(Instant.now());
            refreshTokenRepository.save(existing);
        }
    }

}
