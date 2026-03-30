package com.mrpaulwoods.equipment.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String username;
    private String password;
    private String dataDir = "data";
    private String smtpUser;
    private String smtpPass;
    private String smtpFrom;
    private String appUrl = "http://localhost:8080";
    private String emailRecipient;
}
