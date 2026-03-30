package com.mrpaulwoods.equipment.backend.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IdGeneratorTest {

    @Test
    void generate_returns7CharString() {
        String id = IdGenerator.generate();
        assertThat(id).hasSize(7);
    }

    @Test
    void generate_containsOnlyLowercaseAlphanumeric() {
        String id = IdGenerator.generate();
        assertThat(id).matches("[a-z0-9]{7}");
    }

    @Test
    void generate_producesUniqueValues() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            ids.add(IdGenerator.generate());
        }
        // Expect near-zero collisions across 1000 IDs
        assertThat(ids).hasSizeGreaterThan(995);
    }
}
