package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.repository.RefreshTokenRepository;
import com.mrpaulwoods.equipment.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Profile("test")
public class TestResetController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @DeleteMapping("/reset")
    @Transactional
    public ResponseEntity<Void> reset() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
