package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcedureRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 4000) String steps,
        @Size(max = 500) String requiredTools,
        @Min(1) int intervalDays
) {
}
