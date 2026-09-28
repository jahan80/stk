package com.starterkit.ticket.shared.infrastructure.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String publicKeyPath = "classpath:keys/jwt-public.pem";
    private String issuer = "starterkit-auth";
    private String audience = "starterkit-services";
}
