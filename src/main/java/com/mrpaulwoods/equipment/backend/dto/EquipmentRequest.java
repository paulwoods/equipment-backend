package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EquipmentRequest(
        @NotBlank String manufacturer,
        @NotBlank String modelNumber,
        String serialNumber,
        String assetTag,
        String location,
        @NotNull EquipmentStatus status,
        String description,
        @NotNull LocalDate purchaseDate
) {
}
