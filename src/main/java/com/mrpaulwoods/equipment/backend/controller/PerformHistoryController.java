package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.PerformRequest;
import com.mrpaulwoods.equipment.backend.dto.PerformResponse;
import com.mrpaulwoods.equipment.backend.service.PerformHistoryService;
import com.mrpaulwoods.equipment.backend.service.RoleTier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/equipment/{equipmentId}/procedures/{procedureId}/history")
@RequiredArgsConstructor
@Tag(name = "Perform History", description = "Record and retrieve maintenance performance history")
public class PerformHistoryController {

    private final PerformHistoryService performHistoryService;

    @Operation(summary = "List all perform records for a procedure")
    @PreAuthorize(RoleTier.READ)
    @GetMapping
    public ResponseEntity<List<PerformResponse>> getHistory(@PathVariable UUID equipmentId,
                                                                @PathVariable UUID procedureId) {
        return ResponseEntity.ok(performHistoryService.getHistory(equipmentId, procedureId));
    }

    @Operation(summary = "Record a new maintenance performance entry")
    @PreAuthorize(RoleTier.WRITE)
    @PostMapping
    public ResponseEntity<PerformResponse> record(@PathVariable UUID equipmentId,
                                                        @PathVariable UUID procedureId,
                                                        @Valid @RequestBody PerformRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performHistoryService.record(equipmentId, procedureId, request));
    }
}
