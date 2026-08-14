package com.mrpaulwoods.equipment.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleIdTokenConfigTest {

    private static final String CLIENT_ID = "our-app.apps.googleusercontent.com";

    private GoogleIdTokenConfig config;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        config = new GoogleIdTokenConfig();
        appProperties = new AppProperties();
        appProperties.setGoogleClientId(CLIENT_ID);
    }

    private Jwt.Builder token() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("1234567890")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS));
    }

    @Test
    void issuerValidator_acceptsBothSpellingsGoogleUses() {
        for (String issuer : List.of("https://accounts.google.com", "accounts.google.com")) {
            Jwt jwt = token().claim("iss", issuer).build();
            assertThat(config.issuerValidator().validate(jwt).hasErrors())
                    .as("issuer %s", issuer)
                    .isFalse();
        }
    }

    @Test
    void issuerValidator_rejectsAnyOtherIssuer() {
        Jwt jwt = token().claim("iss", "https://accounts.evil.example").build();

        assertThat(config.issuerValidator().validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void issuerValidator_rejectsAMissingIssuer() {
        Jwt jwt = token().claim("email", "someone@example.com").build();

        assertThat(config.issuerValidator().validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void audienceValidator_acceptsOurOwnClientId() {
        Jwt jwt = token().audience(List.of(CLIENT_ID)).build();

        OAuth2TokenValidatorResult result = config.audienceValidator(appProperties).validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void audienceValidator_rejectsATokenMintedForAnotherApplication() {
        Jwt jwt = token().audience(List.of("someone-else.apps.googleusercontent.com")).build();

        assertThat(config.audienceValidator(appProperties).validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void audienceValidator_rejectsAMissingAudience() {
        Jwt jwt = token().claim("email", "someone@example.com").build();

        assertThat(config.audienceValidator(appProperties).validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void audienceValidator_rejectsEverythingWhenNoClientIdIsConfigured() {
        appProperties.setGoogleClientId("");
        Jwt jwt = token().audience(List.of(CLIENT_ID)).build();

        assertThat(config.audienceValidator(appProperties).validate(jwt).hasErrors()).isTrue();
    }
}
