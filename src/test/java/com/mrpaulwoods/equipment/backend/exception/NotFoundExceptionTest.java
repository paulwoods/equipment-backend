package com.mrpaulwoods.equipment.backend.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotFoundExceptionTest {

    @Test
    void equipmentNotFoundException_containsId() {
        NotFoundException ex = new NotFoundException("Equipment", "eq-42");
        assertThat(ex.getMessage()).isEqualTo("Equipment not found: eq-42");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    void procedureNotFoundException_containsId() {
        NotFoundException ex = new NotFoundException("Procedure", "proc-7");
        assertThat(ex.getMessage()).isEqualTo("Procedure not found: proc-7");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
