package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.util.Role;

import java.util.UUID;

public record UserResponse(UUID id, String email, Role role) {
}
