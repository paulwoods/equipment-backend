package com.mrpaulwoods.equipment.backend.dto;

import java.time.LocalDate;
import java.util.UUID;

public record PerformCreateResponse(UUID id, LocalDate date, String notes) {
}
