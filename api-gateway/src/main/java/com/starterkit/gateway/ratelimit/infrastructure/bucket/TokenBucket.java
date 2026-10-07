package com.starterkit.gateway.ratelimit.infrastructure.bucket;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe token bucket with sub-token (fractional) refill.
 *
 * Tokens are stored as scaled longs (× SCALE) so we can express
 * fractional rates (e.g. 5 req / 60s = 0.0833 tok/sec) with
 * lock-free atomics.
 *
 * SCALE = 1_000_000 gives micro-token precision.
 *
 * Refill uses millisecond-precision elapsed time and always advances
 * lastRefillMs to `now`, so no time is lost across calls.
 *
 * Fixes:
 *   - integer-division bug in the old rate calc (5/60 == 0)
 *   - partial-second drift in the old refill timestamp
 */
public class TokenBucket {

    private static final long SCALE = 1_000_000L;

    private final long capacityScaled;
    private final double refillPerMs;

    private final AtomicLong tokensScaled;
    private final AtomicLong lastRefillMs;

    public TokenBucket(long capacity, int requestsPerWindow, int windowSeconds) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (requestsPerWindow <= 0 || windowSeconds <= 0) {
            throw new IllegalArgumentException(
                    "requestsPerWindow and windowSeconds must be > 0");
        }

        this.capacityScaled = capacity * SCALE;
        this.refillPerMs = (double) requestsPerWindow
                / ((double) windowSeconds * 1000.0);

        this.tokensScaled = new AtomicLong(capacityScaled);
        this.lastRefillMs = new AtomicLong(System.currentTimeMillis());
    }

    public boolean tryConsume() {
        refill();
        long oneScaled = SCALE;
        while (true) {
            long current = tokensScaled.get();
            if (current < oneScaled) return false;
            if (tokensScaled.compareAndSet(current, current - oneScaled)) return true;
        }
    }

    public long getAvailableTokens() {
        refill();
        return tokensScaled.get() / SCALE;
    }

    public long getSecondsUntilNextToken() {
        refill();
        long current = tokensScaled.get();
        if (current >= SCALE) return 0;

        long missing = SCALE - current;
        double missingTokens = (double) missing / SCALE;

        if (refillPerMs <= 0.0) return 60;

        double ms = missingTokens / refillPerMs;
        return Math.max(1L, (long) Math.ceil(ms / 1000.0));
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long last = lastRefillMs.get();
        if (now <= last) return;
        if (!lastRefillMs.compareAndSet(last, now)) return;

        long elapsedMs = now - last;
        long toAdd = (long) (elapsedMs * refillPerMs * SCALE);
        if (toAdd <= 0) return;

        long current = tokensScaled.get();
        long next = current + toAdd;
        if (next > capacityScaled) next = capacityScaled;
        tokensScaled.set(next);
    }
}
