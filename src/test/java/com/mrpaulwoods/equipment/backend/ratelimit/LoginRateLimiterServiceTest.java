package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterServiceTest {

    private AtomicLong nanos;
    private LoginRateLimiterService service;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.setLoginMaxAttempts(3);
        appProperties.setLoginLockoutDurationMs(60_000L);
        nanos = new AtomicLong();
        Ticker ticker = nanos::get;
        service = new LoginRateLimiterService(appProperties, new WindowedCounterFactory(ticker));
    }

    @Test
    void tryAcquire_upToMaxAttempts_isAllowed() {
        for (int i = 0; i < 3; i++) {
            assertThat(service.tryAcquire("1.2.3.4", "user@example.com")).isTrue();
        }
    }

    @Test
    void tryAcquire_pastMaxAttempts_isBlocked() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire("1.2.3.4", "user@example.com");
        }
        assertThat(service.tryAcquire("1.2.3.4", "user@example.com")).isFalse();
    }

    @Test
    void tryAcquire_countsTheAttemptItChecks() {
        // No separate recordFailure call: the budget is spent by tryAcquire itself,
        // so there is no window between the check and the record.
        service.tryAcquire("1.2.3.4", "user@example.com");
        service.tryAcquire("1.2.3.4", "user@example.com");
        service.tryAcquire("1.2.3.4", "user@example.com");
        assertThat(service.tryAcquire("1.2.3.4", "user@example.com")).isFalse();
    }

    @Test
    void oneIpRotatingEmails_isBlockedByTheIpCounter() {
        assertThat(service.tryAcquire("1.2.3.4", "one@example.com")).isTrue();
        assertThat(service.tryAcquire("1.2.3.4", "two@example.com")).isTrue();
        assertThat(service.tryAcquire("1.2.3.4", "three@example.com")).isTrue();
        assertThat(service.tryAcquire("1.2.3.4", "four@example.com")).isFalse();
    }

    @Test
    void oneEmailRotatingIps_isBlockedByTheEmailCounter() {
        assertThat(service.tryAcquire("1.1.1.1", "user@example.com")).isTrue();
        assertThat(service.tryAcquire("2.2.2.2", "user@example.com")).isTrue();
        assertThat(service.tryAcquire("3.3.3.3", "user@example.com")).isTrue();
        assertThat(service.tryAcquire("4.4.4.4", "user@example.com")).isFalse();
    }

    @Test
    void recordSuccess_clearsTheAccountCounter() {
        service.tryAcquire("1.1.1.1", "user@example.com");
        service.tryAcquire("2.2.2.2", "user@example.com");
        service.tryAcquire("3.3.3.3", "user@example.com");

        service.recordSuccess("3.3.3.3", "user@example.com");

        assertThat(service.tryAcquire("9.9.9.9", "user@example.com")).isTrue();
    }

    @Test
    void recordSuccess_returnsOnlyItsOwnAttemptToTheIpCounter() {
        service.tryAcquire("1.2.3.4", "one@example.com");
        service.tryAcquire("1.2.3.4", "two@example.com");
        service.tryAcquire("1.2.3.4", "three@example.com");

        // A valid account cannot wipe the failures other users of this IP racked up:
        // the success gives back one attempt, not the whole budget.
        service.recordSuccess("1.2.3.4", "three@example.com");

        assertThat(service.tryAcquire("1.2.3.4", "four@example.com")).isTrue();
        assertThat(service.tryAcquire("1.2.3.4", "five@example.com")).isFalse();
    }

    @Test
    void emailCounterIsCaseInsensitive() {
        service.tryAcquire("1.1.1.1", "USER@example.com");
        service.tryAcquire("2.2.2.2", "User@Example.com");
        service.tryAcquire("3.3.3.3", "user@example.com");

        assertThat(service.tryAcquire("4.4.4.4", "user@example.com")).isFalse();
    }

    @Test
    void blockExpiresAfterLockoutWindow() {
        for (int i = 0; i < 3; i++) {
            service.tryAcquire("1.2.3.4", "user@example.com");
        }
        assertThat(service.tryAcquire("1.2.3.4", "user@example.com")).isFalse();

        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(60_001L));

        assertThat(service.tryAcquire("1.2.3.4", "user@example.com")).isTrue();
    }

    @Test
    void nullIpAndEmailAreCounted() {
        assertThat(service.tryAcquire(null, null)).isTrue();
        assertThat(service.tryAcquire(null, null)).isTrue();
        assertThat(service.tryAcquire(null, null)).isTrue();
        assertThat(service.tryAcquire(null, null)).isFalse();
    }
}
