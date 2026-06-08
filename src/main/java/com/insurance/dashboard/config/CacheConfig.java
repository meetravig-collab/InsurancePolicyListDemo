package com.insurance.dashboard.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * In-memory caching for frequently accessed reads (summary stats, listings, detail).
 *
 * Invalidation strategy:
 *   1. Time-based (TTL) — every cache entry expires after {@code cache.ttl-seconds}
 *      (default 60s), bounding staleness even for out-of-band DB changes.
 *   2. Event-based — write operations evict the affected caches immediately
 *      (see @CacheEvict on PolicyServiceImpl.flagPoliciesForReview).
 *
 * Caffeine is a local, single-node cache suitable for this BFF. For multi-instance
 * deployments the CacheManager bean can be swapped for a distributed provider
 * (e.g. Redis) without touching the service-layer annotations.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.ttl-seconds:60}") long ttlSeconds,
            @Value("${cache.max-size:1000}") long maxSize) {

        CaffeineCacheManager manager = new CaffeineCacheManager(
                CacheNames.POLICY_LISTINGS,
                CacheNames.POLICY_BY_ID,
                CacheNames.POLICY_SUMMARY);

        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize)
                .recordStats());

        return manager;
    }
}
