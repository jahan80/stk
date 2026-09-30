package com.starterkit.outbox.infrastructure.publisher;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "outbox")
public class OutboxPublisherProperties {

    /** RabbitMQ exchange to publish to */
    private String exchange = "starterkit.events";

    /** Source service name (e.g. "auth-service") */
    private String sourceService = "unknown-service";

    /** Enable/disable publisher */
    private boolean enabled = true;

    /** Publisher interval in ms */
    private long intervalMs = 5000;

    /** Batch size per poll */
    private int batchSize = 100;

    /** Cleanup: keep PUBLISHED events for N days */
    private int retentionDays = 7;
}
