package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.service.EquipmentService;
import com.mrpaulwoods.equipment.backend.service.ExportService;
import com.mrpaulwoods.equipment.backend.service.ImportService;
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
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public ResponseEntity<Page<EquipmentListResponse>> getAll(
            @PageableDefault(size = 20, sort = "manufacturer", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(equipmentService.getAll(pageable));
    }

    @Operation(summary = "Export all equipment as a downloadable JSON file")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEquipment() throws IOException {
        var equipment = exportService.exportAll();
        byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(equipment);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"equipment-export-" + timestamp + ".json\"");
        return ResponseEntity.ok().headers(headers).body(json);
    }

    @Operation(summary = "Import equipment from a JSON file")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importEquipment(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ImportEquipmentException("Import file is empty");
        }
        List<ImportRequest.EquipmentImport> items;
        try {
            items = objectMapper.readValue(
                    file.getInputStream(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ImportRequest.EquipmentImport.class)
            );
        } catch (StreamReadException e) {
            throw new ImportEquipmentException("Invalid JSON in import file: " + e.getOriginalMessage());
        }
        return ResponseEntity.ok(importService.importEquipment(items));
    }

    @Operation(summary = "Get a single equipment record by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public ResponseEntity<EquipmentDetailResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(equipmentService.getById(id));
    }

    @Operation(summary = "Create a new equipment record")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EquipmentCreateResponse> create(@Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(request));
    }

    @Operation(summary = "Update an existing equipment record")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EquipmentUpdateResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody EquipmentRequest request) {
        return ResponseEntity.ok(equipmentService.update(id, request));
    }

    @Operation(summary = "Delete an equipment record")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        equipmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
