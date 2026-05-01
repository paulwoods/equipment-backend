package com.mrpaulwoods.equipment.backend.dto;

import java.util.UUID;

public record ProcedureResponse(
        UUID id,
        String name,
        String description,
        String steps,
        String requiredTools,
        int intervalDays
) {
}
