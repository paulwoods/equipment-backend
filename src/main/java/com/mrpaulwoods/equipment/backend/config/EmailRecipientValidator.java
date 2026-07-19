package com.mrpaulwoods.equipment.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Fails startup when the dashboard email has no recipient.
 *
 * <p>Without this the misconfiguration surfaces only when someone sends a
 * dashboard email — either by clicking the button, or by waiting for the
 * Saturday scheduler, which logs the failure and moves on. Both are a long way
 * from the deploy that caused it.
 */
@Component
@RequiredArgsConstructor
public class EmailRecipientValidator implements CommandLineRunner {

    private final AppProperties appProperties;

    static void validate(String recipient) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalStateException(
                    "APP_EMAIL_RECIPIENT is not set. It is the address the dashboard summary email is sent to.");
        }
        if (!recipient.contains("@")) {
            throw new IllegalStateException(
                    "APP_EMAIL_RECIPIENT is not an email address (got \"" + recipient + "\").");
        }
    }

    @Override
    public void run(String... args) {
        validate(appProperties.getEmailRecipient());
    }
}
