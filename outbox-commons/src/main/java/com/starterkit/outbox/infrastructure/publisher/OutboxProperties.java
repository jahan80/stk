package com.starterkit.outbox.infrastructure.publisher;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Transactional Outbox Pattern.
 *
 * Binds to the `outbox.*` prefix:
 *
 *   outbox.enabled            - master switch (default: true)
 *   outbox.exchange           - RabbitMQ exchange (default: starterkit.events)
 *   outbox.source-service     - this service's name for event sourcing
 *   outbox.retention-days     - cleanup retention for PUBLISHED events
 *
 *   outbox.publisher.enabled      - scheduler switch
 *   outbox.publisher.interval-ms  - scheduler fixed delay
 *   outbox.publisher.batch-size   - events per poll cycle
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {

    /** RabbitMQ exchange to publish to */
    private String exchange = "starterkit.events";

    /** Source service name (e.g. "auth-service") */
    private String sourceService = "unknown-service";

    /** Master enable/disable for the entire outbox infrastructure */
    private boolean enabled = true;

    /** Cleanup: keep PUBLISHED events for N days */
    private int retentionDays = 7;

    /** Publisher-specific settings */
    private final Publisher publisher = new Publisher();

    @Getter
    @Setter
    public static class Publisher {

        /** Enable/disable the scheduled publisher */
        private boolean enabled = true;

        /** Scheduler delay in milliseconds */
        private long intervalMs = 5000;

        /** Maximum events fetched per poll cycle */
        private int batchSize = 100;
    }
}
