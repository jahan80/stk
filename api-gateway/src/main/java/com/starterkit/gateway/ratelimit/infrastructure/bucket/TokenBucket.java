package com.starterkit.gateway.ratelimit.infrastructure.bucket;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token bucket implementation.
 *
 * Thread-safe for single key (lock-free via AtomicLong).
 */
public class TokenBucket {

    private final long capacity;
    private final long refillRatePerSecond;

    private final AtomicLong tokens;
    private final AtomicLong lastRefillTimestamp;

    public TokenBucket(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
    }

    /**
     * Try to consume 1 token.
     * @return true if consumed, false otherwise
     */
    public boolean tryConsume() {
        refill();

        long current = tokens.get();
        while (current > 0) {
            if (tokens.compareAndSet(current, current - 1)) {
                return true;
            }
            current = tokens.get();
        }
        return false;
    }

    /**
     * Get current token count (without consuming).
     */
    public long getAvailableTokens() {
        refill();
        return tokens.get();
    }

    /**
     * Get seconds until next token available.
     */
    public long getSecondsUntilNextToken() {
        refill();
        if (tokens.get() > 0) {
            return 0;
        }
        return Math.max(1, refillRatePerSecond == 0 ? 60 : 60 / refillRatePerSecond);
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long last = lastRefillTimestamp.get();
        long elapsed = now - last;

        if (elapsed < 1000) {
            return;
        }

        long tokensToAdd = (elapsed / 1000) * refillRatePerSecond;

        if (tokensToAdd > 0) {
            if (lastRefillTimestamp.compareAndSet(last, last + (tokensToAdd / refillRatePerSecond) * 1000)) {
                long current = tokens.get();
                long newValue = Math.min(capacity, current + tokensToAdd);
                tokens.compareAndSet(current, newValue);
            }
        }
    }
}
