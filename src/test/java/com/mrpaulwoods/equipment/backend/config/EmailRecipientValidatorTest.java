package com.mrpaulwoods.equipment.backend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailRecipientValidatorTest {

    @Test
    void validate_withAddress_passes() {
        assertThatCode(() -> EmailRecipientValidator.validate("ops@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_nullRecipient_throws() {
        assertThatThrownBy(() -> EmailRecipientValidator.validate(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_EMAIL_RECIPIENT");
    }

    @Test
    void validate_blankRecipient_throws() {
        assertThatThrownBy(() -> EmailRecipientValidator.validate("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_EMAIL_RECIPIENT");
    }

    @Test
    void validate_withoutAtSign_throws() {
        assertThatThrownBy(() -> EmailRecipientValidator.validate("not-an-address"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not an email address");
    }
}
