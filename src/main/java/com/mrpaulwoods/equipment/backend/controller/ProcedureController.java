package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.service.ProcedureService;
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
@RequestMapping("/api/v1/equipment/{equipmentId}/procedures")
@RequiredArgsConstructor
@Tag(name = "Procedures", description = "Maintenance procedures for a piece of equipment")
public class ProcedureController {

    private final ProcedureService procedureService;

    @Operation(summary = "List all procedures for an equipment record")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<List<ProcedureListResponse>> getAll(@PathVariable UUID equipmentId) {
        return ResponseEntity.ok(procedureService.getAllForEquipment(equipmentId));
    }

    @Operation(summary = "Get a single procedure by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{procedureId}")
    public ResponseEntity<ProcedureDetailResponse> getById(@PathVariable UUID equipmentId,
                                                           @PathVariable UUID procedureId) {
        return ResponseEntity.ok(procedureService.getById(equipmentId, procedureId));
    }

    @Operation(summary = "Create a new procedure for an equipment record")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProcedureCreateResponse> create(@PathVariable UUID equipmentId,
                                                          @Valid @RequestBody ProcedureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(procedureService.create(equipmentId, request));
    }

    @Operation(summary = "Update an existing procedure")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{procedureId}")
    public ResponseEntity<ProcedureUpdateResponse> update(@PathVariable UUID equipmentId,
                                                          @PathVariable UUID procedureId,
                                                          @Valid @RequestBody ProcedureRequest request) {
        return ResponseEntity.ok(procedureService.update(equipmentId, procedureId, request));
    }

    @Operation(summary = "Delete a procedure")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{procedureId}")
    public ResponseEntity<Void> delete(@PathVariable UUID equipmentId, @PathVariable UUID procedureId) {
        procedureService.delete(equipmentId, procedureId);
        return ResponseEntity.noContent().build();
    }
}
