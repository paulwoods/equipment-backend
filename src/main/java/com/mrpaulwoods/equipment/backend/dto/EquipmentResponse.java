package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.EquipmentStatus;

import java.time.LocalDate;
import java.util.UUID;

public record EquipmentResponse(
        UUID id,
        String manufacturer,
        String modelNumber,
        String serialNumber,
        String assetTag,
        String location,
        EquipmentStatus status,
        String description,
        LocalDate purchaseDate
) {
}
