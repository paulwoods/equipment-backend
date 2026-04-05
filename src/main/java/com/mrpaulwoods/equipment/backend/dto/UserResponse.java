package com.mrpaulwoods.equipment.backend.dto;

import com.mrpaulwoods.equipment.backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private Role role;
}
