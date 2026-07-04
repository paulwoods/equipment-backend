package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds {@link WindowedCounter}s, injecting the shared {@link Ticker} so time is
 * controlled in one place (system in prod, a fake in tests).
 */
@Component
public class WindowedCounterFactory {

    private final Ticker ticker;

    public WindowedCounterFactory(Ticker ticker) {
        this.ticker = ticker;
    }

    public <K> WindowedCounter<K> create(Duration window) {
        return new WindowedCounter<>(window, ticker);
    }
}
