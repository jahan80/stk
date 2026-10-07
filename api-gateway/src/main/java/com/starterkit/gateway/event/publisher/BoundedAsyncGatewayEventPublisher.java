package com.starterkit.gateway.event.publisher;

import com.starterkit.gateway.config.RabbitMqConfig;
import com.starterkit.gateway.event.GatewayEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous, bounded gateway event publisher.
 *
 * Rationale:
 *   Gateway events are observability data, not business state.
 *   Blocking a user request to publish audit events would harm latency.
 *   Instead we enqueue events into a bounded queue and publish them
 *   on a dedicated worker thread.
 *
 * Loss semantics:
 *   - Queue is bounded (default 10_000). When full, events are DROPPED.
 *   - Drop is logged at WARN and counted in GatewayEventPublisherStats.
 *   - This bounds memory and prevents gateway stalls under broker outage.
 *
 * Retry:
 *   - Worker retries each event up to MAX_ATTEMPTS times with small backoff.
 *   - After MAX_ATTEMPTS the event is dropped (counted as publishFailure).
 *
 * Selection:
 *   Set gateway.events.publisher=async (default) or =sync.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "gateway.events.publisher",
        havingValue = "async",
        matchIfMissing = true
)
public class BoundedAsyncGatewayEventPublisher implements GatewayEventPublisher {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_BACKOFF_MS = 100L;

    private final RabbitTemplate rabbitTemplate;
    private final GatewayEventPublisherStats stats;

    private final int queueCapacity;
    private final BlockingQueue<GatewayEventMessage> queue;
    private ExecutorService worker;
    private volatile boolean running = true;

    public BoundedAsyncGatewayEventPublisher(
            RabbitTemplate rabbitTemplate,
            GatewayEventPublisherStats stats,
            @Value("${gateway.events.queue-capacity:10000}") int queueCapacity
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.stats = stats;
        this.queueCapacity = queueCapacity;
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
    }

    @PostConstruct
    void start() {
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "gw-event-publisher");
            t.setDaemon(true);
            return t;
        });
        worker.submit(this::runLoop);

        log.info("BoundedAsyncGatewayEventPublisher started: queueCapacity={}", queueCapacity);
    }

    @PreDestroy
    void stop() {
        running = false;
        if (worker != null) {
            worker.shutdownNow();
            try {
                worker.awaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("BoundedAsyncGatewayEventPublisher stopped");
    }

    @Override
    public void publish(GatewayEvent event) {
        GatewayEventMessage message;
        try {
            message = GatewayEventMessage.from(event);
        } catch (Exception ex) {
            log.error("Failed to build gateway event message: type={}", event.eventType(), ex);
            stats.incWorkerErrors();
            return;
        }

        boolean accepted = queue.offer(message);
        if (accepted) {
            stats.incEnqueued();
        } else {
            stats.incDroppedQueueFull();
            log.warn("Gateway event queue FULL ({}), dropping: type={}, routingKey={}",
                    queueCapacity, event.eventType(), event.routingKey());
        }
    }

    // =====================================================
    // Worker loop
    // =====================================================

    private void runLoop() {
        while (running) {
            try {
                GatewayEventMessage message = queue.poll(1, TimeUnit.SECONDS);
                if (message == null) continue;
                publishWithRetry(message);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                stats.incWorkerErrors();
                log.error("Unexpected worker error", ex);
            }
        }
    }

    private void publishWithRetry(GatewayEventMessage message) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXCHANGE,
                        deriveRoutingKey(message),
                        message
                );
                stats.incPublished();
                log.debug("Gateway event published: type={}, attempt={}",
                        message.getEventType(), attempt);
                return;
            } catch (Exception ex) {
                if (attempt < MAX_ATTEMPTS) {
                    log.debug("Gateway event publish failed (attempt {}/{}): {}",
                            attempt, MAX_ATTEMPTS, ex.getMessage());
                    try {
                        Thread.sleep(RETRY_BACKOFF_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    stats.incPublishFailures();
                    log.error("Gateway event GIVEN UP after {} attempts: type={}",
                            MAX_ATTEMPTS, message.getEventType(), ex);
                }
            }
        }
    }

    /**
     * Routing key derivation. GatewayEventMessage does not carry the
     * routing key, so we derive it from the event type.
     *
     *   REQUEST_RECEIVED  -> gateway.request.received
     *   RESPONSE_SENT     -> gateway.response.sent
     */
    private String deriveRoutingKey(GatewayEventMessage message) {
        return switch (message.getEventType()) {
            case "REQUEST_RECEIVED" -> "gateway.request.received";
            case "RESPONSE_SENT"    -> "gateway.response.sent";
            default -> "gateway." + message.getEventType().toLowerCase();
        };
    }
}
