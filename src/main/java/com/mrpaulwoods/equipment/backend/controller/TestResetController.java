package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.repository.RefreshTokenRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Profile("test")
@Tag(name = "Test Utilities", description = "Test-profile-only utilities for resetting database state")
public class TestResetController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Operation(summary = "Delete all users and refresh tokens (test profile only)")
    @DeleteMapping("/reset")
    @Transactional
    public ResponseEntity<Void> reset() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
