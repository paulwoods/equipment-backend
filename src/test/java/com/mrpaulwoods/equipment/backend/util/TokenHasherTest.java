package com.mrpaulwoods.equipment.backend.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenHasherTest {

    // NIST test vector: SHA-256("abc")
    private static final String SHA256_OF_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    // NIST test vector: SHA-256("")
    private static final String SHA256_OF_EMPTY = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    void sha256Hex_knownVector_matchesNistTestValue() {
        assertThat(TokenHasher.sha256Hex("abc")).isEqualTo(SHA256_OF_ABC);
    }

    @Test
    void sha256Hex_emptyString_matchesKnownHash() {
        assertThat(TokenHasher.sha256Hex("")).isEqualTo(SHA256_OF_EMPTY);
    }

    @Test
    void sha256Hex_isDeterministic_sameInputYieldsSameOutput() {
        String value = "some-refresh-token-value";

        String first = TokenHasher.sha256Hex(value);
        String second = TokenHasher.sha256Hex(value);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void sha256Hex_differentInputs_yieldDifferentOutputs() {
        assertThat(TokenHasher.sha256Hex("token-a")).isNotEqualTo(TokenHasher.sha256Hex("token-b"));
    }

    @Test
    void sha256Hex_outputIsLowercaseHexOfExpectedLength() {
        String hex = TokenHasher.sha256Hex("any-value");

        // SHA-256 digest is 32 bytes -> 64 hex characters.
        assertThat(hex).hasSize(64);
        assertThat(hex).matches("[0-9a-f]{64}");
    }

    @Test
    void sha256Hex_nullInput_throwsNullPointerException() {
        assertThatThrownBy(() -> TokenHasher.sha256Hex(null))
                .isInstanceOf(NullPointerException.class);
    }
}
