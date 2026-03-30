package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProcedureRequest(
        @NotBlank String name,
        String description,
        @NotBlank String steps,
        String requiredTools,
        @Min(1) int intervalDays
) {
}
