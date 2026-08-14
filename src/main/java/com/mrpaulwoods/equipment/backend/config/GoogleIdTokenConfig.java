package com.mrpaulwoods.equipment.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.Set;

/**
 * Verification of Google ID tokens presented to {@code POST /api/v1/auth/google}.
 * <p>
 * The decoder pulls Google's signing keys from their published JWK Set and
 * caches them, so key rotation needs no redeployment. On top of the default
 * signature and expiry checks it enforces the two claims that make an ID token
 * ours rather than some other relying party's: {@code iss} must be Google, and
 * {@code aud} must be our own OAuth client ID.
 */
@Configuration
public class GoogleIdTokenConfig {

    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    // Google has issued both spellings over the years and still documents both as valid.
    private static final Set<String> GOOGLE_ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    /**
     * Keys are fetched lazily on the first decode, so this bean is cheap to create
     * even when Google sign-in is switched off.
     */
    @Bean
    public JwtDecoder googleIdTokenDecoder(AppProperties appProperties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                issuerValidator(),
                audienceValidator(appProperties)
        ));
        return decoder;
    }

    // Package-private so the validators can be exercised without reaching Google's JWKS.
    OAuth2TokenValidator<Jwt> issuerValidator() {
        OAuth2Error error = new OAuth2Error(
                "invalid_token", "The iss claim is not a Google issuer", null);
        // Read the raw claim rather than Jwt#getIssuer, which coerces to a URL and so
        // throws on the bare "accounts.google.com" spelling this validator must accept.
        return jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            // Set.of rejects a null argument outright, so guard before the lookup.
            return issuer != null && GOOGLE_ISSUERS.contains(issuer)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(error);
        };
    }

    /**
     * Without this an attacker could sign in with a genuine Google ID token minted
     * for a completely different application.
     */
    OAuth2TokenValidator<Jwt> audienceValidator(AppProperties appProperties) {
        OAuth2Error error = new OAuth2Error(
                "invalid_token", "The aud claim is not this application's client ID", null);
        return jwt -> {
            String clientId = appProperties.getGoogleClientId();
            List<String> audience = jwt.getAudience();
            if (clientId == null || clientId.isBlank() || audience == null || !audience.contains(clientId)) {
                return OAuth2TokenValidatorResult.failure(error);
            }
            return OAuth2TokenValidatorResult.success();
        };
    }
}
