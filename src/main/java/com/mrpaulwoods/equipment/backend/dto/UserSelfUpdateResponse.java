package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.Role;

import java.util.UUID;

public record UserSelfUpdateResponse(
        UUID id,
        String name,
        String email,
        Role role
) {
}
