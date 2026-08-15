package com.mrpaulwoods.equipment.backend.ratelimit;

import com.mrpaulwoods.equipment.backend.config.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

/**
 * Throttles login attempts on two independent axes: the source IP and the target
 * account. A single composite (ip, email) counter would throttle neither — an
 * attacker rotates emails to keep the IP counter fresh, or rotates IPs to keep the
 * account counter fresh — so each dimension gets its own counter and either one
 * hitting the threshold blocks the attempt.
 */
@Service
public class LoginRateLimiterService {

    private final AppProperties appProperties;
    private final WindowedCounter<String> ipAttempts;
    private final WindowedCounter<String> emailAttempts;

    public LoginRateLimiterService(AppProperties appProperties, WindowedCounterFactory counterFactory) {
        this.appProperties = appProperties;
        Duration lockout = Duration.ofMillis(appProperties.getLoginLockoutDurationMs());
        this.ipAttempts = counterFactory.create(lockout);
        this.emailAttempts = counterFactory.create(lockout);
    }

    /**
     * Count this attempt against both axes and report whether it may proceed.
     * Counting and deciding are one step per axis: a separate "is it blocked?" read
     * followed by a later "record the failure" write would let concurrent requests
     * all observe the same under-threshold count and slip through together, and the
     * bcrypt verification between the two makes that window wide.
     *
     * @return false if either axis has now exceeded the attempt budget
     */
    public boolean tryAcquire(String ip, String email) {
        // Both counters are always incremented — no short-circuit, or the second
        // axis would stop counting as soon as the first one blocks.
        boolean ipWithinBudget = ipAttempts.increment(normalizeIp(ip)) <= appProperties.getLoginMaxAttempts();
        boolean emailWithinBudget = emailAttempts.increment(normalizeEmail(email)) <= appProperties.getLoginMaxAttempts();
        return ipWithinBudget && emailWithinBudget;
    }

    /**
     * A proven login. The account's counter is cleared outright; the IP's is only
     * given back the one attempt this login consumed, so a valid account cannot be
     * used to wipe the failures another user of that IP has racked up.
     */
    public void recordSuccess(String ip, String email) {
        ipAttempts.release(normalizeIp(ip));
        emailAttempts.reset(normalizeEmail(email));
    }

    private String normalizeIp(String ip) {
        return ip == null ? "" : ip;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.toLowerCase(Locale.ROOT);
    }
}
