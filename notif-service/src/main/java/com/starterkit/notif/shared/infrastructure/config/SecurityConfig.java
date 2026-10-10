package com.starterkit.notif.shared.infrastructure.config;

import com.starterkit.notif.shared.infrastructure.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * notif-service security.
 *
 * Public:
 *   /notify/email, /notify/sms, /notify/push  -> service-to-service (internal only via gateway)
 *   /v3/api-docs, /swagger-ui                 -> docs
 *
 * JWT required:
 *   /notify/me/**                             -> user-scoped in-app notifications
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public API: used by other services through the gateway
                // (auth checks happen at the gateway)
                .requestMatchers(HttpMethod.POST, "/notify/email", "/notify/sms", "/notify/push").permitAll()

                // Everything under /notify/me requires a valid JWT
                .requestMatchers("/notify/me/**").authenticated()

                // Admin-ish read endpoints require JWT (checked by @PreAuthorize)
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
