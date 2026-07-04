package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterServiceTest {

    private AppProperties appProperties;
    private AtomicLong nanos;
    private LoginRateLimiterService service;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.setLoginMaxAttempts(3);
        appProperties.setLoginLockoutDurationMs(60_000L);
        nanos = new AtomicLong();
        Ticker ticker = nanos::get;
        service = new LoginRateLimiterService(appProperties, new WindowedCounterFactory(ticker));
    }

    @Test
    void isBlocked_beforeAnyAttempts_returnsFalse() {
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isFalse();
    }

    @Test
    void isBlocked_afterMaxFailures_returnsTrue() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("1.2.3.4", "user@example.com");
        }
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isTrue();
    }

    @Test
    void isBlocked_belowThreshold_returnsFalse() {
        service.recordFailure("1.2.3.4", "user@example.com");
        service.recordFailure("1.2.3.4", "user@example.com");
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isFalse();
    }

    @Test
    void recordSuccess_clearsPriorFailures() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("1.2.3.4", "user@example.com");
        }
        service.recordSuccess("1.2.3.4", "user@example.com");
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isFalse();
    }

    @Test
    void keysAreIsolatedByIpAndEmail() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("1.2.3.4", "user@example.com");
        }
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isTrue();
        assertThat(service.isBlocked("9.9.9.9", "user@example.com")).isFalse();
        assertThat(service.isBlocked("1.2.3.4", "other@example.com")).isFalse();
    }

    @Test
    void keyIsCaseInsensitiveOnEmail() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("1.2.3.4", "USER@example.com");
        }
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isTrue();
    }

    @Test
    void blockExpiresAfterLockoutWindow() {
        for (int i = 0; i < 3; i++) {
            service.recordFailure("1.2.3.4", "user@example.com");
        }
        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isTrue();

        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(60_001L));

        assertThat(service.isBlocked("1.2.3.4", "user@example.com")).isFalse();
    }
}
