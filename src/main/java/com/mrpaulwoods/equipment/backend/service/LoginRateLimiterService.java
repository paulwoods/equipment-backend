package com.mrpaulwoods.equipment.backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginRateLimiterService {

    private final AppProperties appProperties;
    private final Cache<Key, AtomicInteger> attempts;

    @Autowired
    public LoginRateLimiterService(AppProperties appProperties) {
        this(appProperties, Ticker.systemTicker());
    }

    LoginRateLimiterService(AppProperties appProperties, Ticker ticker) {
        this.appProperties = appProperties;
        this.attempts = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(appProperties.getLoginLockoutDurationMs()))
                .ticker(ticker)
                .maximumSize(10_000)
                .build();
    }

    public boolean isBlocked(String ip, String email) {
        AtomicInteger count = attempts.getIfPresent(key(ip, email));
        return count != null && count.get() >= appProperties.getLoginMaxAttempts();
    }

    public void recordFailure(String ip, String email) {
        attempts.asMap()
                .computeIfAbsent(key(ip, email), _ -> new AtomicInteger())
                .incrementAndGet();
    }

    public void recordSuccess(String ip, String email) {
        attempts.invalidate(key(ip, email));
    }

    private Key key(String ip, String email) {
        String normalizedEmail = email == null ? "" : email.toLowerCase(Locale.ROOT);
        String normalizedIp = ip == null ? "" : ip;
        return new Key(normalizedIp, normalizedEmail);
    }

    private record Key(String ip, String email) {
        private Key {
            Objects.requireNonNull(ip);
            Objects.requireNonNull(email);
        }
    }
}
