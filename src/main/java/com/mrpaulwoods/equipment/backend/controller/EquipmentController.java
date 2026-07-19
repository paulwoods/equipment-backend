package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.dto.EquipmentResponse;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.service.EquipmentService;
import com.mrpaulwoods.equipment.backend.service.ExportService;
import com.mrpaulwoods.equipment.backend.service.ImportService;
import com.mrpaulwoods.equipment.backend.service.RoleTier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment", description = "CRUD, import, and export for equipment records")
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final ExportService exportService;
    private final ImportService importService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "List all equipment (paginated)")
    @PreAuthorize(RoleTier.READ)
    @GetMapping
    public ResponseEntity<Page<EquipmentResponse>> getAll(
            @PageableDefault(size = 20, sort = "manufacturer", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(equipmentService.getAll(pageable));
    }

    @Operation(summary = "Export all equipment as a downloadable JSON file")
    @PreAuthorize(RoleTier.WRITE)
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEquipment() throws IOException {
        var equipment = exportService.exportAll();
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(equipment);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Colon-free pattern: ISO_LOCAL_DATE_TIME produces ':' which is illegal in Windows filenames.
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"equipment-export-" + timestamp + ".json\"");
        return ResponseEntity.ok().headers(headers).body(json);
    }

    @Operation(summary = "Import equipment from a JSON file")
    @PreAuthorize(RoleTier.WRITE)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importEquipment(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(importService.importEquipment(file.getInputStream()));
    }

    @Operation(summary = "Get a single equipment record by ID")
    @PreAuthorize(RoleTier.READ)
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentService.getById(id));
    }

    @Operation(summary = "Create a new equipment record")
    @PreAuthorize(RoleTier.WRITE)
    @PostMapping
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @Operation(summary = "Update an existing equipment record")
    @PreAuthorize(RoleTier.WRITE)
    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.update(id, request));
    }

    @Operation(summary = "Delete an equipment record")
    @PreAuthorize(RoleTier.WRITE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
