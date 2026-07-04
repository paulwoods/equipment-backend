package com.mrpaulwoods.equipment.backend.ratelimit;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ForgotPasswordRateLimiterService {

    private final AppProperties appProperties;
    private final WindowedCounter<String> attempts;

    public ForgotPasswordRateLimiterService(AppProperties appProperties, WindowedCounterFactory counterFactory) {
        this.appProperties = appProperties;
        this.attempts = counterFactory.create(
                Duration.ofMillis(appProperties.getForgotPasswordWindowMs()));
    }

    public boolean isBlocked(String ip) {
        return attempts.count(ip) >= appProperties.getForgotPasswordMaxAttempts();
    }

    public void recordRequest(String ip) {
        attempts.increment(ip);
    }
}
