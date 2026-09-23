package com.starterkit.auth.shared.infrastructure.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * RSA private key location (for signing)
     */
    private String privateKeyPath = "classpath:keys/jwt-private.pem";

    /**
     * RSA public key location (for verification)
     */
    private String publicKeyPath = "classpath:keys/jwt-public.pem";

    /**
     * Access token time-to-live in seconds (default: 15 minutes)
     */
    private long accessTokenTtlSeconds = 900;

    /**
     * Refresh token time-to-live in seconds (default: 7 days)
     */
    private long refreshTokenTtlSeconds = 604800;

    /**
     * JWT issuer claim value
     */
    private String issuer = "starterkit-auth";

    /**
     * JWT audience claim value
     */
    private String audience = "starterkit-services";
}