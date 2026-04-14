package com.mrpaulwoods.equipment.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtSecretValidator implements CommandLineRunner {

    static final String DEFAULT_PLACEHOLDER = "changeme-use-a-32-char-secret-here!!!!";
    static final int MIN_BYTE_LENGTH = 32;
    static final int MIN_DISTINCT_CHARS = 16;

    private final AppProperties appProperties;

    static void validate(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(errorMessage("APP_JWT_SECRET is not set"));
        }
        if (secret.equals(DEFAULT_PLACEHOLDER)) {
            throw new IllegalStateException(errorMessage("APP_JWT_SECRET is still the built-in placeholder"));
        }
        int byteLength = secret.getBytes(StandardCharsets.UTF_8).length;
        if (byteLength < MIN_BYTE_LENGTH) {
            throw new IllegalStateException(errorMessage(
                    "APP_JWT_SECRET must be at least " + MIN_BYTE_LENGTH + " bytes (got " + byteLength + ")"));
        }
        if (secret.chars().distinct().count() < MIN_DISTINCT_CHARS) {
            throw new IllegalStateException(errorMessage(
                    "APP_JWT_SECRET has too little entropy (fewer than " + MIN_DISTINCT_CHARS + " distinct characters)"));
        }
    }

    private static String errorMessage(String reason) {
        return reason + ". Generate a strong secret with: openssl rand -base64 48";
    }

    @Override
    public void run(String... args) {
        validate(appProperties.getJwtSecret());
    }
}
