package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Shared import/export shape for an {@code Equipment} and its nested
 * procedures/history. The same record is used both to parse an import file
 * and to serialize an export, so an exported file can be re-imported without
 * transformation.
 */
public record EquipmentTransfer(
        @Size(max = 36) String id,
        @NotBlank @Size(max = 100) String manufacturer,
        @NotBlank @Size(max = 100) String modelNumber,
        @Size(max = 100) String serialNumber,
        @Size(max = 100) String assetTag,
        @Size(max = 255) String location,
        EquipmentStatus status,
        @Size(max = 2000) String description,
        @NotNull LocalDate purchaseDate,
        List<@Valid ProcedureTransfer> procedures
) {
}
