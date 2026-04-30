package com.mrpaulwoods.equipment.backend.dto;

import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String email, Set<RoleResponse> roles) {
}
