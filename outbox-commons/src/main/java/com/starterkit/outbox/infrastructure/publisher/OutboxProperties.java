package com.starterkit.outbox.infrastructure.publisher;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    private String exchange = "starterkit.events";

    private String sourceService = "unknown-service";

    private boolean enabled = true;

    private int retentionDays = 7;

    /**
     * Unique instance identifier for claim tracking.
     * Defaults to hostname + PID. Override via OUTBOX_INSTANCE_ID env var
     * in multi-replica deployments.
     */
    private String instanceId = defaultInstanceId();

    /**
     * How long a claimed event stays locked before being reclaimed.
     * Should be longer than the max time to publish (RabbitMQ confirm timeout).
     */
    private int leaseSeconds = 60;

    private final Publisher publisher = new Publisher();

    private static String defaultInstanceId() {
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "unknown-host";
        }
        long pid = ProcessHandle.current().pid();
        return host + "-" + pid;
    }

    @Getter
    @Setter
    public static class Publisher {

        private boolean enabled = true;

        private long intervalMs = 5000;

        private int batchSize = 100;

        /** How often the reclaim job runs (resets expired CLAIMED → PENDING). */
        private long reclaimIntervalMs = 60_000;
    }
}
