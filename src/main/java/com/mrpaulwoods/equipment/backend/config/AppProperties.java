package com.mrpaulwoods.equipment.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String jwtSecret = "changeme-use-a-32-char-secret-here!!";
    private long jwtExpirationMs = 3_600_000L;
    private String smtpUser;
    private String smtpPass;
    private String smtpFrom;
    private String appUrl = "http://localhost:8080";
    private String emailRecipient;
}
