package com.mrpaulwoods.equipment.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String jwtSecret = "changeme-use-a-32-char-secret-here!!!!";
    private long jwtExpirationMs = 3_600_000L;
    private String smtpFrom;
    // The public URL users visit. Frontend and API are same-origin behind the
    // reverse proxy, so this is the base for every user-facing link we email.
    private String appUrl = "http://localhost:8080";
    private String emailRecipient;
    private Boolean cookieSecure;
    private int refreshTokenDays = 7;
    private int loginMaxAttempts = 5;
    private long loginLockoutDurationMs = 15L * 60L * 1000L;
    private int forgotPasswordMaxAttempts = 3;
    private long forgotPasswordWindowMs = 3_600_000L;
    private int apiRateLimitMaxRequests = 100;
    private long apiRateLimitWindowMs = 60_000L;
    private String corsAllowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";
    // OAuth client ID from the Google Cloud console. Blank disables Google sign-in
    // entirely: the config endpoint reports it off and the login endpoint refuses.
    // Public by design — the browser sends it to Google — so it is safe to serve.
    private String googleClientId = "";
}
