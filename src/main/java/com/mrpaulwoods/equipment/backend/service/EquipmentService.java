package com.mrpaulwoods.equipment.backend.service;

import com.mrpaulwoods.equipment.backend.dto.*;
import com.mrpaulwoods.equipment.backend.entity.Equipment;
import com.mrpaulwoods.equipment.backend.exception.EquipmentNotFoundException;
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

    public Page<EquipmentListResponse> getAll(Pageable pageable) {
        return equipmentRepository.findAll(pageable).map(this::toListResponse);
    }

    public EquipmentDetailResponse getById(UUID id) {
        return equipmentRepository.findById(id)
                .map(this::toDetailResponse)
                .orElseThrow(() -> new EquipmentNotFoundException(id.toString()));
    }

    Equipment getEntityById(UUID id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new EquipmentNotFoundException(id.toString()));
    }

    @Transactional
    public EquipmentCreateResponse create(EquipmentRequest request) {
        var equipment = toEntity(request);
        return toCreateResponse(equipmentRepository.save(equipment));
    }

    @Transactional
    public EquipmentUpdateResponse update(UUID id, EquipmentRequest request) {
        var existing = getEntityById(id);
        existing.setManufacturer(request.manufacturer());
        existing.setModelNumber(request.modelNumber());
        existing.setSerialNumber(request.serialNumber());
        existing.setAssetTag(request.assetTag());
        existing.setLocation(request.location());
        existing.setStatus(request.status());
        existing.setDescription(request.description());
        existing.setPurchaseDate(request.purchaseDate());
        return toUpdateResponse(equipmentRepository.save(existing));
    }

    @Transactional
    public void delete(UUID id) {
        if (!equipmentRepository.existsById(id)) {
            throw new EquipmentNotFoundException(id.toString());
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

    private EquipmentListResponse toListResponse(Equipment e) {
        return new EquipmentListResponse(e.getId(), e.getManufacturer(), e.getModelNumber(),
                e.getSerialNumber(), e.getAssetTag(), e.getLocation(), e.getStatus(),
                e.getDescription(), e.getPurchaseDate());
    }

    private EquipmentDetailResponse toDetailResponse(Equipment e) {
        return new EquipmentDetailResponse(e.getId(), e.getManufacturer(), e.getModelNumber(),
                e.getSerialNumber(), e.getAssetTag(), e.getLocation(), e.getStatus(),
                e.getDescription(), e.getPurchaseDate());
    }

    private EquipmentCreateResponse toCreateResponse(Equipment e) {
        return new EquipmentCreateResponse(e.getId(), e.getManufacturer(), e.getModelNumber(),
                e.getSerialNumber(), e.getAssetTag(), e.getLocation(), e.getStatus(),
                e.getDescription(), e.getPurchaseDate());
    }

    private EquipmentUpdateResponse toUpdateResponse(Equipment e) {
        return new EquipmentUpdateResponse(e.getId(), e.getManufacturer(), e.getModelNumber(),
                e.getSerialNumber(), e.getAssetTag(), e.getLocation(), e.getStatus(),
                e.getDescription(), e.getPurchaseDate());
    }
}
