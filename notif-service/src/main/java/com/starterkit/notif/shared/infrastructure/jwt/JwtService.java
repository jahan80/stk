package com.starterkit.notif.shared.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties props;
    private final ResourceLoader resourceLoader;

    private PublicKey publicKey;

    private PublicKey getKey() {
        if (publicKey == null) {
            synchronized (this) {
                if (publicKey == null) publicKey = load();
            }
        }
        return publicKey;
    }

    private PublicKey load() {
        try {
            Resource r = resourceLoader.getResource(props.getPublicKeyPath());
            String key;
            try (InputStream is = r.getInputStream()) {
                key = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                     .replace("-----END PUBLIC KEY-----", "")
                     .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(key);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT public key", e);
        }
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .requireIssuer(props.getIssuer())
                .requireAudience(props.getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserId(Claims c) { return Long.parseLong(c.getSubject()); }
    public String getUsername(Claims c) { return c.get("username", String.class); }
    public String getEmail(Claims c) { return c.get("email", String.class); }

    @SuppressWarnings("unchecked")
    public List<String> getRoles(Claims c) {
        Object v = c.get("roles");
        return v instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissions(Claims c) {
        Object v = c.get("permissions");
        return v instanceof List<?> l ? l.stream().map(String::valueOf).toList() : List.of();
    }

    public boolean isAccessToken(Claims c) {
        return "access".equals(c.get("type", String.class));
    }
}
