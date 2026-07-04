package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A per-key count that expires a fixed window after the key's first increment. The
 * Caffeine mechanics (cache build, expire-after-write, atomic increment) live here so
 * the rate-limit policy in each caller stays thin. Generic in the key type: callers
 * own their key (see {@code LoginRateLimiterService}'s composite key).
 *
 * <p>The window is anchored at entry creation: subsequent increments mutate the count
 * in place (invisible to the cache) and do not push the expiry out. Once the window
 * elapses the key is evicted and the next increment starts a fresh window.
 */
public final class WindowedCounter<K> {

    private static final int MAX_KEYS = 10_000;

    private final Cache<K, AtomicInteger> counts;

    public WindowedCounter(Duration window, Ticker ticker) {
        this.counts = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .ticker(ticker)
                .maximumSize(MAX_KEYS)
                .build();
    }

    /** Current count for the key; 0 if absent. A read — does not extend the window. */
    public int count(K key) {
        AtomicInteger count = counts.getIfPresent(key);
        return count == null ? 0 : count.get();
    }

    /** Increment and return the new count. Starts the window on the key's first increment. */
    public int increment(K key) {
        return counts.asMap()
                .computeIfAbsent(key, _ -> new AtomicInteger())
                .incrementAndGet();
    }

    /** Clear the key's count. */
    public void reset(K key) {
        counts.invalidate(key);
    }
}
