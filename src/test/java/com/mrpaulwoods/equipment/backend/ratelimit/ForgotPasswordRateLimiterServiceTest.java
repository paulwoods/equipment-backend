package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class ForgotPasswordRateLimiterServiceTest {

    private AppProperties appProperties;
    private AtomicLong nanos;
    private ForgotPasswordRateLimiterService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setForgotPasswordMaxAttempts(3);
        appProperties.setForgotPasswordWindowMs(60_000L);
        nanos = new AtomicLong();
        Ticker ticker = nanos::get;
        service = new ForgotPasswordRateLimiterService(appProperties, new WindowedCounterFactory(ticker));
    }

    @Test
    void isBlocked_beforeAnyAttempts_returnsFalse() {
        assertThat(service.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void isBlocked_afterMaxRequests_returnsTrue() {
        for (int i = 0; i < 3; i++) {
            service.recordRequest("1.2.3.4");
        }
        assertThat(service.isBlocked("1.2.3.4")).isTrue();
    }

    @Test
    void isBlocked_belowThreshold_returnsFalse() {
        service.recordRequest("1.2.3.4");
        service.recordRequest("1.2.3.4");
        assertThat(service.isBlocked("1.2.3.4")).isFalse();
    }

    @Test
    void keysAreIsolatedByIp() {
        for (int i = 0; i < 3; i++) {
            service.recordRequest("1.2.3.4");
        }
        assertThat(service.isBlocked("1.2.3.4")).isTrue();
        assertThat(service.isBlocked("9.9.9.9")).isFalse();
    }

    @Test
    void blockExpiresAfterWindow() {
        for (int i = 0; i < 3; i++) {
            service.recordRequest("1.2.3.4");
        }
        assertThat(service.isBlocked("1.2.3.4")).isTrue();

        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(60_001L));

        assertThat(service.isBlocked("1.2.3.4")).isFalse();
    }
}
