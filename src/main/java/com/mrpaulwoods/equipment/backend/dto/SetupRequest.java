package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetupRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
