package com.mrpaulwoods.equipment.backend.dto;

import java.util.Set;
import java.util.UUID;

public record UserSelfUpdateResponse(UUID id, String name, String email, Set<RoleResponse> roles) {
}
