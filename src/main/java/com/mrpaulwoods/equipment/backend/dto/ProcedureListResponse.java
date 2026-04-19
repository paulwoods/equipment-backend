package com.mrpaulwoods.equipment.backend.dto;

import java.util.UUID;

public record ProcedureListResponse(
        UUID id,
        String name,
        String description,
        String steps,
        String requiredTools,
        int intervalDays
) {
}
