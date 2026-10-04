package com.starterkit.auth.shared.infrastructure.config;

import com.starterkit.auth.shared.infrastructure.internal.InternalAuthFilter;
import com.starterkit.auth.shared.infrastructure.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalAuthFilter internalAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // InternalAuthFilter runs FIRST for /internal/**.
                // It enforces X-Internal-Token before JWT.
                .addFilterBefore(
                        internalAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/refresh",
                                "/auth/logout",
                                "/auth/email/verify",
                                "/auth/email/resend-verification",
                                "/auth/mobile/verify",
                                "/auth/mobile/resend-verification",
                                "/auth/password/forgot",
                                "/auth/password/reset"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/auth/configurations",
                                "/auth/configurations/**"
                        ).permitAll()

                        // /internal/** is guarded by InternalAuthFilter
                        // (X-Internal-Token). permitAll here so Spring Security
                        // does not additionally require a JWT.
                        .requestMatchers("/internal/**").permitAll()

                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
