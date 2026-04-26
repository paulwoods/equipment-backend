package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record UserPasswordChangeRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
