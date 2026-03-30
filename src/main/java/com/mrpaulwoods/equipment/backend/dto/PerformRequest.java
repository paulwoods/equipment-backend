package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PerformRequest(
        @NotNull LocalDate date,
        String notes
) {
}
