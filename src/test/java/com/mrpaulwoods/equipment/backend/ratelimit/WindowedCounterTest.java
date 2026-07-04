package com.mrpaulwoods.equipment.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class WindowedCounterTest {

    private AtomicLong nanos;
    private WindowedCounter<String> counter;

    @BeforeEach
    void setUp() {
        nanos = new AtomicLong();
        Ticker ticker = nanos::get;
        counter = new WindowedCounter<>(Duration.ofMillis(60_000L), ticker);
    }

    @Test
    void count_absentKey_returnsZero() {
        assertThat(counter.count("a")).isZero();
    }

    @Test
    void increment_returnsNewCount() {
        assertThat(counter.increment("a")).isEqualTo(1);
        assertThat(counter.increment("a")).isEqualTo(2);
        assertThat(counter.count("a")).isEqualTo(2);
    }

    @Test
    void reset_clearsCount() {
        counter.increment("a");
        counter.increment("a");
        counter.reset("a");
        assertThat(counter.count("a")).isZero();
    }

    @Test
    void keysAreIsolated() {
        counter.increment("a");
        counter.increment("a");
        counter.increment("b");
        assertThat(counter.count("a")).isEqualTo(2);
        assertThat(counter.count("b")).isEqualTo(1);
    }

    @Test
    void countExpiresAfterWindow() {
        counter.increment("a");
        assertThat(counter.count("a")).isEqualTo(1);

        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(60_001L));

        assertThat(counter.count("a")).isZero();
    }

    @Test
    void windowIsAnchoredAtFirstIncrement_notExtendedByLaterOnes() {
        // The window runs from the entry's creation. Later increments mutate the count
        // in place (invisible to the cache), so they do NOT push the expiry out.
        counter.increment("a");

        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(59_000L));
        assertThat(counter.increment("a")).isEqualTo(2);

        // Still expires 60s after the FIRST increment, despite the second one 1s ago.
        nanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(1_001L));
        assertThat(counter.count("a")).isZero();
    }
}
