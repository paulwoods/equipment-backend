package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Shared import/export shape for a single {@code Perform} history entry.
 * The same record is used both to parse an import file and to serialize an
 * export, so a re-imported export is byte-for-byte compatible.
 */
public record PerformTransfer(
        @Size(max = 36) String id,
        @NotNull LocalDate date,
        @Size(max = 2000) String notes
) {
}
