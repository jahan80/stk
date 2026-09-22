package com.starterkit.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

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
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        // ========================================
                        // Swagger / OpenAPI  —  هر دو حالت با و بدون /**
                        // ========================================
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

                        // ========================================
                        // CORS preflight
                        // ========================================
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ========================================
                        // Auth
                        // ========================================
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login"
                        ).permitAll()

                        // ========================================
                        // Configurations
                        // ========================================
                        .requestMatchers(
                                "/auth/configurations",
                                "/auth/configurations/**"
                        ).permitAll()

                        // ========================================
                        // بقیه نیاز به احراز هویت
                        // ========================================
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}