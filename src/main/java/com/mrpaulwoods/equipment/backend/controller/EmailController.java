package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> sendDashboard() {
        try {
            emailService.sendDashboardEmail();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to send email"));
        }
    }
}
