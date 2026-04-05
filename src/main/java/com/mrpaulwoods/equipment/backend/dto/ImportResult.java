package com.mrpaulwoods.equipment.backend.dto;

public record ImportResult(
        int equipmentImported,
        int proceduresImported,
        int historyImported
) {
}
