package com.starterkit.gateway.event.publisher;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory counters for gateway event publishing.
 *
 * Gateway events (REQUEST_RECEIVED / RESPONSE_SENT) are OBSERVABILITY
 * data, not business state. If a few events are lost due to backpressure
 * or broker downtime, business correctness is unaffected. These counters
 * make the loss rate visible to operators via /gateway/events/stats.
 *
 * NOTE: not persisted. Counters reset on restart. For long-term metrics,
 * expose these to a real metrics system in a future step.
 */
@Component
@Getter
public class GatewayEventPublisherStats {

    private final AtomicLong enqueued = new AtomicLong();
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong droppedQueueFull = new AtomicLong();
    private final AtomicLong publishFailures = new AtomicLong();
    private final AtomicLong lastErrorAt = new AtomicLong(); // epoch ms
    private final AtomicLong workerErrors = new AtomicLong();

    public void incEnqueued()       { enqueued.incrementAndGet(); }
    public void incPublished()      { published.incrementAndGet(); }
    public void incDroppedQueueFull() { droppedQueueFull.incrementAndGet(); }
    public void incPublishFailures()  {
        publishFailures.incrementAndGet();
        lastErrorAt.set(System.currentTimeMillis());
    }
    public void incWorkerErrors()   { workerErrors.incrementAndGet(); }

    public Snapshot snapshot() {
        long now = System.currentTimeMillis();
        long lastErr = lastErrorAt.get();

        return new Snapshot(
                enqueued.get(),
                published.get(),
                droppedQueueFull.get(),
                publishFailures.get(),
                workerErrors.get(),
                lastErr > 0 ? Instant.ofEpochMilli(lastErr) : null,
                lastErr > 0 ? (now - lastErr) / 1000 : null
        );
    }

    public record Snapshot(
            long enqueued,
            long published,
            long droppedQueueFull,
            long publishFailures,
            long workerErrors,
            Instant lastErrorAt,
            Long lastErrorSecondsAgo
    ) {}
}
