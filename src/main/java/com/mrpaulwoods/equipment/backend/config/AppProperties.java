package com.mrpaulwoods.equipment.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String jwtSecret = "changeme-use-a-32-char-secret-here!!!!";
    private long jwtExpirationMs = 3_600_000L;
    private String smtpFrom;
    private String appUrl = "http://localhost:8080";
    private String frontendUrl;
    private String emailRecipient;
    private Boolean cookieSecure;
    private int loginMaxAttempts = 5;
    private long loginLockoutDurationMs = 15L * 60L * 1000L;
    private int forgotPasswordMaxAttempts = 3;
    private long forgotPasswordWindowMs = 3_600_000L;
    private int apiRateLimitMaxRequests = 100;
    private long apiRateLimitWindowMs = 60_000L;
    private String corsAllowedOrigins = "http://localhost:5173,http://127.0.0.1:5173";

    public String getFrontendUrl() {
        return (frontendUrl != null && !frontendUrl.isBlank()) ? frontendUrl : appUrl;
    }
}
