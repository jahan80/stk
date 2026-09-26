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

    private String privateKeyPath = "classpath:keys/jwt-private.pem";
    private String publicKeyPath = "classpath:keys/jwt-public.pem";
    private long accessTokenTtlSeconds = 900;
    private long refreshTokenTtlSeconds = 604800;
    private String issuer = "starterkit-auth";
    private String audience = "starterkit-services";
}
