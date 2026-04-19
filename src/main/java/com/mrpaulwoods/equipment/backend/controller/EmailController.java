package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Manually trigger email notifications")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "Send the dashboard summary email to all users")
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
