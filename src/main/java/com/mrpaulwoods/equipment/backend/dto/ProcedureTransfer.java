package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Shared import/export shape for a {@code Procedure} and its history. The
 * same record is used both to parse an import file and to serialize an
 * export, so a re-imported export is byte-for-byte compatible.
 */
public record ProcedureTransfer(
        @Size(max = 36) String id,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 4000) String steps,
        @Size(max = 500) String requiredTools,
        @NotNull @Min(1) Integer intervalDays,
        List<@Valid PerformTransfer> history
) {
}
