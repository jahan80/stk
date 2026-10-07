package com.starterkit.notif.infrastructure.retry;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Retry policy for failed notification deliveries.
 *
 * Backoff schedule (seconds): 30 → 60 → 300 → 1800 → 7200 (cap)
 *   attempt 1 → wait 30s   (first retry)
 *   attempt 2 → wait 60s
 *   attempt 3 → wait 5m
 *   attempt 4 → wait 30m
 *   attempt 5 → wait 2h
 *   attempt 6+ → stop (row stays FAILED, alert-able)
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "notif.retry")
public class NotifRetryProperties {

    private boolean enabled = true;

    /** Max delivery attempts before giving up. */
    private int maxAttempts = 6;

    /** How often the retry job polls. */
    private long intervalMs = 30_000L;

    /** Batch size per poll. */
    private int batchSize = 50;

    /** Lease duration for a claimed row (seconds). */
    private int leaseSeconds = 120;

    /** Instance ID for claim tracking. */
    private String instanceId = defaultInstanceId();

    private static String defaultInstanceId() {
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown-host";
        }
        long pid = ProcessHandle.current().pid();
        return "notif-" + host + "-" + pid;
    }
}
