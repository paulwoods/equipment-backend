package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.EquipmentRequest;
import com.mrpaulwoods.equipment.backend.dto.EquipmentResponse;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.NotFoundException;
import com.mrpaulwoods.equipment.backend.repository.EquipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;

    public Page<EquipmentResponse> getAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable).map(this::toResponse);
    }

    public EquipmentResponse getById(UUID id) {
        return equipmentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Equipment", id.toString()));
    }

    Equipment getEntityById(UUID id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Equipment", id.toString()));
    }

    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        var equipment = toEntity(request);
        return toResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentResponse update(UUID id, EquipmentRequest request) {
        var existing = getEntityById(id);
        existing.setManufacturer(request.manufacturer());
        existing.setModelNumber(request.modelNumber());
        existing.setSerialNumber(request.serialNumber());
        existing.setAssetTag(request.assetTag());
        existing.setLocation(request.location());
        existing.setStatus(request.status());
        existing.setDescription(request.description());
        existing.setPurchaseDate(request.purchaseDate());
        return toResponse(equipmentRepository.save(existing));
    }

    @Transactional
    public void delete(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new NotFoundException("Equipment", id.toString());
        }
        equipmentRepository.deleteById(id);
    }

    private Equipment toEntity(EquipmentRequest request) {
        var e = new Equipment();
        e.setManufacturer(request.manufacturer());
        e.setModelNumber(request.modelNumber());
        e.setSerialNumber(request.serialNumber());
        e.setAssetTag(request.assetTag());
        e.setLocation(request.location());
        e.setStatus(request.status());
        e.setDescription(request.description());
        e.setPurchaseDate(request.purchaseDate());
        return e;
    }

    private EquipmentResponse toResponse(Equipment e) {
        return new EquipmentResponse(e.getId(), e.getManufacturer(), e.getModelNumber(),
                e.getSerialNumber(), e.getAssetTag(), e.getLocation(), e.getStatus(),
                e.getDescription(), e.getPurchaseDate());
    }
}
