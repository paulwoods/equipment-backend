package com.mrpaulwoods.equipment.backend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTest {

    @Test
    void validate_withStrongSecret_passes() {
        String secret = "Xk9vL2pQwErTyUiO3sDfGhJkZxCvBnM7aSdFgHjKlQwErTyU";
        assertThatCode(() -> JwtSecretValidator.validate(secret)).doesNotThrowAnyException();
    }

    @Test
    void validate_nullSecret_throws() {
        assertThatThrownBy(() -> JwtSecretValidator.validate(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void validate_blankSecret_throws() {
        assertThatThrownBy(() -> JwtSecretValidator.validate("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    void validate_hardcodedDefault_throws() {
        assertThatThrownBy(() -> JwtSecretValidator.validate(JwtSecretValidator.DEFAULT_PLACEHOLDER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void validate_tooShort_throws() {
        String shortSecret = "abcdef0123456789";
        assertThatThrownBy(() -> JwtSecretValidator.validate(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least");
    }

    @Test
    void validate_lowEntropyRepeating_throws() {
        String repeating = "a".repeat(64);
        assertThatThrownBy(() -> JwtSecretValidator.validate(repeating))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entropy");
    }
}
