package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Export DTOs mirroring the {@link ImportRequest} field shape so an exported
 * file can be re-imported without transformation.
 */
public class ExportResponse {

    public record PerformExport(
            String id,
            LocalDate date,
            String notes
    ) {
    }

    public record ProcedureExport(
            String id,
            String name,
            String description,
            String steps,
            String requiredTools,
            Integer intervalDays,
            List<PerformExport> history
    ) {
    }

    public record EquipmentExport(
            String id,
            String manufacturer,
            String modelNumber,
            String serialNumber,
            String assetTag,
            String location,
            EquipmentStatus status,
            String description,
            LocalDate purchaseDate,
            List<ProcedureExport> procedures
    ) {
    }
}
