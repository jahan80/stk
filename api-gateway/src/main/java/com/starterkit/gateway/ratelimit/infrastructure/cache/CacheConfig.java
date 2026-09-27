package com.starterkit.gateway.ratelimit.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${gateway.rate-limit.cache-ttl-seconds:60}")
    private long cacheTtlSeconds;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("activeRateLimits");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
                        .maximumSize(500)
        );
        return cacheManager;
    }
}
