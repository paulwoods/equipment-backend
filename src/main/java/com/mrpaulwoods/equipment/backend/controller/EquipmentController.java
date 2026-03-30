package com.mrpaulwoods.equipment.backend.controller;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.model.Equipment;
import com.mrpaulwoods.equipment.backend.service.EquipmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping
    public List<Equipment> getAll() {
        return equipmentService.getAll();
    }

    @GetMapping("/{id}")
    public Equipment getById(@PathVariable String id) {
        return equipmentService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Equipment create(@Valid @RequestBody EquipmentRequest request) {
        Equipment equipment = toEquipment(request);
        return equipmentService.create(equipment);
    }

    @PutMapping("/{id}")
    public Equipment update(@PathVariable String id, @Valid @RequestBody EquipmentRequest request) {
        Equipment equipment = toEquipment(request);
        return equipmentService.update(id, equipment);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
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
