package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.EmailSendResponse;
import com.mrpaulwoods.equipment.backend.service.EmailService;
import com.mrpaulwoods.equipment.backend.service.RoleTier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Tag(name = "Email", description = "Manually trigger email notifications")
public class EmailController {

    private final EmailService emailService;

    @Operation(summary = "Send the dashboard summary email to all users")
    @PreAuthorize(RoleTier.EMAIL)
    @PostMapping("/dashboard")
    public ResponseEntity<EmailSendResponse> sendDashboard() throws Exception {
        emailService.sendDashboardEmail();
        return ResponseEntity.ok(new EmailSendResponse(true));
    }
}
