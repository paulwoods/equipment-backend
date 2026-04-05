package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;

import java.time.LocalDate;
import java.util.List;

public class ImportRequest {

    public record PerformImport(
            String id,
            LocalDate date,
            String notes
    ) {
    }

    public record ProcedureImport(
            String id,
            String name,
            String description,
            String steps,
            String requiredTools,
            Integer intervalDays,
            List<PerformImport> history
    ) {
    }

    public record EquipmentImport(
            String id,
            String manufacturer,
            String modelNumber,
            String serialNumber,
            String assetTag,
            String location,
            EquipmentStatus status,
            String description,
            LocalDate purchaseDate,
            List<ProcedureImport> procedures
    ) {
    }
}
