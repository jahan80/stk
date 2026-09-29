package com.starterkit.auth.shared.infrastructure.jwt;

import com.starterkit.auth.auth.domain.entity.Permission;
import com.starterkit.auth.auth.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_EMAIL    = "email";
    private static final String CLAIM_ROLES    = "roles";
    private static final String CLAIM_PERMISSIONS = "permissions";
    private static final String CLAIM_TYPE     = "type";

    private static final String TOKEN_TYPE_ACCESS  = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final ResourceLoader resourceLoader;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    // =====================================================
    // Key loading (lazy, cached)
    // =====================================================

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    privateKey = loadPrivateKey();
                }
            }
        }
        return privateKey;
    }

    private PublicKey getPublicKey() {
        if (publicKey == null) {
            synchronized (this) {
                if (publicKey == null) {
                    publicKey = loadPublicKey();
                }
            }
        }
        return publicKey;
    }

    private PrivateKey loadPrivateKey() {
        try {
            String key = readPemFile(jwtProperties.getPrivateKeyPath());
            key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load JWT private key from: " +
                            jwtProperties.getPrivateKeyPath(), e);
        }
    }

    private PublicKey loadPublicKey() {
        try {
            String key = readPemFile(jwtProperties.getPublicKeyPath());
            key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load JWT public key from: " +
                            jwtProperties.getPublicKeyPath(), e);
        }
    }

    private String readPemFile(String path) throws IOException {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // =====================================================
    // Token generation
    // =====================================================

    /**
     * Generate an access token for the user.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getAccessTokenTtlSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLES, List.of(user.getRole().getName()))
                .claim(CLAIM_PERMISSIONS, user.getRole().getPermissions().stream()
                        .map(Permission::getCode)
                        .toList())
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .signWith(getPrivateKey())
                .compact();
    }

    /**
     * Generate a refresh token ID (jti).
     * Note: the actual refresh token value is a random string,
     * not a JWT. It's returned separately and hashed before storage.
     */
    public String generateRefreshTokenJti() {
        return UUID.randomUUID().toString();
    }

    // =====================================================
    // Token parsing
    // =====================================================

    /**
     * Parse and verify a JWT. Returns claims if valid.
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getPublicKey())
                    .requireIssuer(jwtProperties.getIssuer())
                    .requireAudience(jwtProperties.getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException ex) {
            log.debug("JWT parsing failed: {}", ex.getMessage());
            throw ex;
        }
    }

    /**
     * Extract userId from a verified access token.
     */
    public Long getUserId(Claims claims) {
        return Long.parseLong(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get(CLAIM_USERNAME, String.class);
    }

    public String getEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims claims) {
        Object value = claims.get(CLAIM_ROLES);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public String getTokenType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }

    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(claims));
    }
}