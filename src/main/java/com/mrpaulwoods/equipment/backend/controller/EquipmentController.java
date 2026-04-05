package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.dto.ImportRequest;
import com.mrpaulwoods.equipment.backend.dto.ImportResult;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.ImportEquipmentException;
import com.mrpaulwoods.equipment.backend.service.EquipmentService;
import com.mrpaulwoods.equipment.backend.service.ImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;
    private final ImportService importService;
    private final ObjectMapper objectMapper;

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public List<Equipment> getAll() {
        return equipmentService.getAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importEquipment(@RequestParam("file") MultipartFile file) throws IOException {
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
        return importService.importEquipment(items);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public Equipment getById(@PathVariable UUID id) {
        return equipmentService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Equipment create(@Valid @RequestBody EquipmentRequest request) {
        Equipment equipment = toEquipment(request);
        return equipmentService.create(equipment);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Equipment update(@PathVariable UUID id, @Valid @RequestBody EquipmentRequest request) {
        Equipment equipment = toEquipment(request);
        return equipmentService.update(id, equipment);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        equipmentService.delete(id);
    }

    private Equipment toEquipment(EquipmentRequest request) {
        Equipment equipment = new Equipment();
        equipment.setManufacturer(request.manufacturer());
        equipment.setModelNumber(request.modelNumber());
        equipment.setSerialNumber(request.serialNumber());
        equipment.setAssetTag(request.assetTag());
        equipment.setLocation(request.location());
        equipment.setStatus(request.status());
        equipment.setDescription(request.description());
        equipment.setPurchaseDate(request.purchaseDate());
        return equipment;
    }
}
