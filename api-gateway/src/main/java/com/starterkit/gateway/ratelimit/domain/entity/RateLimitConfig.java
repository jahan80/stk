package com.starterkit.gateway.ratelimit.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "rate_limits", schema = "gateway")
@Getter
@Setter
@NoArgsConstructor
public class RateLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path_pattern", nullable = false, length = 200)
    private String pathPattern;

    @Column(length = 10)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false, length = 20)
    private KeyType keyType = KeyType.IP_PATH;

    @Column(name = "requests_per_window", nullable = false)
    private int requestsPerWindow;

    @Column(name = "window_seconds", nullable = false)
    private int windowSeconds;

    @Column(name = "burst_capacity")
    private Integer burstCapacity;

    @Column(name = "default_requests_per_window", nullable = false)
    private int defaultRequestsPerWindow;

    @Column(name = "default_window_seconds", nullable = false)
    private int defaultWindowSeconds;

    @Column(name = "default_burst_capacity")
    private Integer defaultBurstCapacity;

    @Column(name = "default_enabled", nullable = false)
    private boolean defaultEnabled = true;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int priority = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum KeyType {
        IP,
        IP_PATH,
        USER,
        USER_PATH
    }
}
