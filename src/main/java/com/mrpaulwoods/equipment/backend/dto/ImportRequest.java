package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class ImportRequest {

    public record PerformImport(
            @Size(max = 36) String id,
            LocalDate date,
            @Size(max = 2000) String notes
    ) {
    }

    public record ProcedureImport(
            @Size(max = 36) String id,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 4000) String steps,
            @Size(max = 500) String requiredTools,
            Integer intervalDays,
            List<PerformImport> history
    ) {
    }

    public record EquipmentImport(
            @Size(max = 36) String id,
            @NotBlank @Size(max = 100) String manufacturer,
            @NotBlank @Size(max = 100) String modelNumber,
            @Size(max = 100) String serialNumber,
            @Size(max = 100) String assetTag,
            @Size(max = 255) String location,
            EquipmentStatus status,
            @Size(max = 2000) String description,
            LocalDate purchaseDate,
            List<ProcedureImport> procedures
    ) {
    }
}
