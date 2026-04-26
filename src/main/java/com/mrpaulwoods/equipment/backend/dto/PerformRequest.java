package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PerformRequest(
        @NotNull LocalDate date,
        @Size(max = 2000) String notes
) {
}
