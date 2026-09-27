package com.starterkit.gateway.ratelimit.infrastructure.bucket;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class BucketRegistry {

    private final Cache<String, TokenBucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public TokenBucket getOrCreate(String key, long capacity, long refillRate) {
        return buckets.get(key, k -> new TokenBucket(capacity, refillRate));
    }

    public void clear() {
        buckets.invalidateAll();
    }
}
