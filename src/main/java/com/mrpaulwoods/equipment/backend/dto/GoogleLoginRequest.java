package com.mrpaulwoods.equipment.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param credential the ID token issued by Google Identity Services. The upper
 *                   bound is a sanity cap on a signed JWT, not a protocol limit.
 */
public record GoogleLoginRequest(
        @NotBlank @Size(max = 4096) String credential
) {
}
