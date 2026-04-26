package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EquipmentRequest(
        @NotBlank @Size(max = 100) String manufacturer,
        @NotBlank @Size(max = 100) String modelNumber,
        @Size(max = 100) String serialNumber,
        @Size(max = 100) String assetTag,
        @Size(max = 255) String location,
        @NotNull EquipmentStatus status,
        @Size(max = 2000) String description,
        @NotNull LocalDate purchaseDate
) {
}
