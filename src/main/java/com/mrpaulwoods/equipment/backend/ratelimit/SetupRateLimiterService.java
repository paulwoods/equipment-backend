package com.mrpaulwoods.equipment.backend.ratelimit;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Throttles first-run setup attempts per source IP, so the setup token cannot be
 * guessed by volume during the window between a deployment starting and its operator
 * completing setup. The general API filter allows 100 requests a minute, which is
 * ample for that; this is the endpoint-specific brake.
 */
@Service
public class SetupRateLimiterService {

    private final AppProperties appProperties;
    private final WindowedCounter<String> attempts;

    public SetupRateLimiterService(AppProperties appProperties, WindowedCounterFactory counterFactory) {
        this.appProperties = appProperties;
        this.attempts = counterFactory.create(
                Duration.ofMillis(appProperties.getSetupWindowMs()));
    }

    /**
     * Count this attempt and report whether it may proceed. Counting and deciding are
     * one atomic step, so concurrent attempts cannot all pass the same check.
     */
    public boolean tryAcquire(String ip) {
        return attempts.increment(ip == null ? "" : ip) <= appProperties.getSetupMaxAttempts();
    }
}
