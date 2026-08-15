package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class SetupRateLimiterServiceTest {

    private AtomicLong nanos;
    private SetupRateLimiterService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setSetupMaxAttempts(3);
        appProperties.setSetupWindowMs(60_000L);
        nanos = new AtomicLong();
        Ticker ticker = nanos::get;
        service = new SetupRateLimiterService(appProperties, new WindowedCounterFactory(ticker));
    }

    @Test
    void tryAcquire_upToMaxAttempts_isAllowed() {
        for (int i = 0; i < 3; i++) {
            assertThat(service.tryAcquire("1.2.3.4")).isTrue();
        }
    }

    @Test
    void tryAcquire_pastMaxAttempts_isBlocked() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire("1.2.3.4");
        }
        assertThat(service.tryAcquire("1.2.3.4")).isFalse();
    }

    @Test
    void ipsAreCountedSeparately() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire("1.2.3.4");
        }
        assertThat(service.tryAcquire("9.9.9.9")).isTrue();
    }

    @Test
    void nullIpIsCounted() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire(null);
        }
        assertThat(service.tryAcquire(null)).isFalse();
    }

    @Test
    void blockExpiresAfterTheWindow() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire("1.2.3.4");
        }
        assertThat(service.tryAcquire("1.2.3.4")).isFalse();

        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(60_001L));

        assertThat(service.tryAcquire("1.2.3.4")).isTrue();
    }
}
