package com.mrpaulwoods.equipment.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ForgotPasswordRateLimiterService {

    private final AppProperties appProperties;
    private final Cache<String, AtomicInteger> attempts;

    @Autowired
    public ForgotPasswordRateLimiterService(AppProperties appProperties) {
        this(appProperties, Ticker.systemTicker());
    }

    ForgotPasswordRateLimiterService(AppProperties appProperties, Ticker ticker) {
        this.appProperties = appProperties;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(appProperties.getForgotPasswordWindowMs()))
                .ticker(ticker)
                .maximumSize(10_000)
                .build();
    }

    public boolean isBlocked(String ip) {
        AtomicInteger count = attempts.getIfPresent(ip);
        return count != null && count.get() >= appProperties.getForgotPasswordMaxAttempts();
    }

    public void recordRequest(String ip) {
        attempts.asMap()
                .computeIfAbsent(ip, _ -> new AtomicInteger())
                .incrementAndGet();
    }
}
