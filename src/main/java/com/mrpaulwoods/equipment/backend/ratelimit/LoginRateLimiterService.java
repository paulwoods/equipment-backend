package com.mrpaulwoods.equipment.backend.ratelimit;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@Service
public class LoginRateLimiterService {

    private final AppProperties appProperties;
    private final WindowedCounter<Key> attempts;

    public LoginRateLimiterService(AppProperties appProperties, WindowedCounterFactory counterFactory) {
        this.appProperties = appProperties;
        this.attempts = counterFactory.create(
                Duration.ofMillis(appProperties.getLoginLockoutDurationMs()));
    }

    public boolean isBlocked(String ip, String email) {
        return attempts.count(key(ip, email)) >= appProperties.getLoginMaxAttempts();
    }

    public void recordFailure(String ip, String email) {
        attempts.increment(key(ip, email));
    }

    public void recordSuccess(String ip, String email) {
        attempts.reset(key(ip, email));
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
