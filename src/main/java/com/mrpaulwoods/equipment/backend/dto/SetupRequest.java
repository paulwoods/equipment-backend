package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        // Checked against app.setup-token in the controller, not here: a bean-validation
        // failure would answer 400 and tell a guesser which field was wrong.
        @Size(max = 256) String setupToken
) {
}
