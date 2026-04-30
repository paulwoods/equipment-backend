package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.DashboardItem;
import com.mrpaulwoods.equipment.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Maintenance due-date summary")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "List equipment with upcoming or overdue maintenance")
    @PreAuthorize("hasAnyRole('USER', 'EDIT', 'ADMIN', 'SYSTEM_ADMIN')")
    @GetMapping
    public List<DashboardItem> getDashboard() {
        return dashboardService.getDashboardItems();
    }
}
